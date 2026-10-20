# Principes OOP et design patterns

Ce document relie chaque principe orienté objet et chaque pattern à **l'endroit exact du code** où il est appliqué,
avec la raison du choix. Les diagrammes sont dans [`UML.md`](UML.md).

## 1. Les quatre piliers

| Pilier | Où | Détail |
|---|---|---|
| **Encapsulation** | `Entity`, `Character`, `Ability`, `Stun`, `HitStop` | état privé (`alive`, `timer`, `freeze`…), accès par méthodes ; les sous-classes n'accèdent qu'à ce qui est `protected` (`aim`, `position`, `timer`/`cooldown`) |
| **Héritage** | `Entity → Character → Player / Enemy → ShooterEnemy / SkullKnight` | le comportement commun (position, hitbox, mort, déplacement par pas) est écrit une seule fois |
| **Polymorphisme** | `Entity.update/draw`, `Enemy.think`, `Ability.steer/act`, `ShapeEffect` | `GameScreen` boucle sur `List<Enemy>` sans connaître le type ; `GameRenderer` boucle sur `List<ShapeEffect>` |
| **Abstraction** | `Entity`, `Character`, `Enemy`, `Ability` (abstraites), `ShapeEffect` (interface) | on programme contre des contrats, pas contre des classes concrètes |

## 2. Principes SOLID

| Principe | Application |
|---|---|
| **S** — responsabilité unique | `GameScreen` ne fait que **coordonner** : le chargement est dans `MapLoader`, le rendu dans `GameRenderer`, les balles dans `BulletSystem`, les temps dans `HitStop/SlowMotion/DeathSequence`. `Enemy` délègue perception (`EnemyVision`), déplacement (`EnemyAI`), assommement (`Stun`), tir (`EnemyWeapon`). Les derniers commits (« Refactor : HitStop, BulletSystem, ShapeEffect extraits… ») vont dans ce sens. |
| **O** — ouvert/fermé | Ajouter un ennemi = **nouvelle sous-classe d'`Enemy`**, sans toucher à `Enemy` (voir `SkullKnight`, ajouté après `ShooterEnemy`). Ajouter une capacité = nouvelle sous-classe d'`Ability` dans le tableau `abilities`. Ajouter un effet = nouvelle implémentation de `ShapeEffect`. |
| **L** — substitution de Liskov | Tout `Enemy` est utilisable là où l'on attend un `Enemy` : `World.enemies`, `BulletSystem.hitEnemies`, `MeleeAttack.strike`, `ParryAbility.act` n'ont aucun `instanceof`. |
| **I** — ségrégation des interfaces | `ShapeEffect` ne comporte que 3 méthodes (`update/draw/clear`) ; `Disposable` (libGDX) est implémentée seulement par ce qui a des ressources natives. |
| **D** — inversion des dépendances | `BulletSystem` ne connaît pas `GameScreen` : il reçoit un `Runnable onPlayerKilled` (`death::start`). `Player` ne connaît ni la caméra ni le `HitStop` : il expose des `poll…()` que `GameScreen` lit. |

## 3. Design patterns

### 3.1 Template Method — `Enemy.update`

*Problème* : tous les ennemis suivent le même cycle (mort ? sonné ? sinon agir), mais agissent différemment.

*Solution* : `Enemy.update` fixe l'**algorithme** ; les sous-classes remplissent les **étapes**.

```java
// Enemy.java — squelette (final de fait : les sous-classes ne le redéfinissent pas)
public void update(float delta, World world) {
    stateTime += delta;
    if (!isAlive()) { deathTime += delta; ai.setSeesPlayer(false); onDeadTick(); return; }
    tick(delta);                                   // hook : recharges
    if (stun.advance(delta, this, world.moveLayers)) { onStunned(); return; }   // hook
    think(delta, world);                           // étape abstraite
}
```

Étapes : `think` (abstraite) · hooks vides par défaut : `tick`, `onDeadTick`, `onStunned`, `onParried`.
`ShooterEnemy` et `SkullKnight` n'implémentent que ce qui les distingue.

*Même idée* dans `Character.takeDamage()` (fixe : vivant ? invincible ? → mort) avec les hooks `isInvincible()` et `onDeath()`.

### 3.2 Composition / Strategy-like — `Player` et `Ability`

*Problème* : le joueur avait toutes ses capacités dans une seule classe géante.

*Solution* : une abstraction `Ability` (durée + recharge + `steer` + `act`), trois implémentations, et `Player` les **compose** :

```java
private final Ability[] abilities = {dash, melee, parry};   // ordre = priorité sur le déplacement

for (Ability a : abilities) a.tick(delta);                  // chaque capacité gère son minuteur
for (Ability a : abilities) { Vector2 v = a.steer(world); if (v != null) return v; }
```

C'est une **chaîne de responsabilité** légère : la première capacité qui impose un mouvement l'emporte (dash > coup > parry).
Ajouter une capacité, c'est écrire une classe et l'ajouter au tableau.

### 3.3 Façade / Context Object — `World`

`World` expose en un seul objet tout ce dont une entité a besoin pour se mettre à jour (calques, joueur, ennemis, balles, effets,
`makeNoise`). Sans lui, chaque `update(…)` aurait cinq paramètres. Inconvénient assumé : `World` a des champs publics ;
c'est un *objet de contexte*, pas un modèle métier.

### 3.4 Observer (simplifié) — drapeaux à lecture unique et callback

Deux variantes, choisies pour éviter les dépendances circulaires :

- **Drapeaux consommés** : `Player.pollDashStart()`, `pollParrySuccess()`, `pollAttackHit()` renvoient vrai **une seule fois**.
  `GameScreen` les lit après `player.update` et déclenche secousse / `HitStop`. Le joueur ignore qui l'écoute.
- **Callback** : `new BulletSystem(world, death::start)` — le système de balles notifie la mort du joueur sans connaître l'écran.

Pas d'`EventBus` : avec 3–4 événements, c'est plus lisible et sans coût.

### 3.5 Static Factory Method — `DirectionalAnimation`

`DirectionalAnimation.sideRight(...)` / `sideLeft(...)` construisent une animation 4 directions à partir d'une feuille de
sprites (la gauche étant la droite retournée, ou l'inverse). Le constructeur est **privé** : seules les fabriques nommées
expriment l'intention. `withFrameTime` renvoie une copie rythmée autrement (marche → course).

### 3.6 State — machines à états implicites

Pas de classes `State` dédiées (volontairement), mais des états explicites par minuteurs et drapeaux :

| Machine | État stocké | Voir |
|---|---|---|
| IA ennemie | `alerted`, `walking`, `chasing`, `Stun.timer`, `alive` | `UML.md §9` |
| Attaque du chevalier | `attackTime` (< 0 = au repos) + `hitDone` | `SkullKnight.updateAttack` |
| Attaque du tireur | `winding`, `volleysLeft`, `recoilTimer`, `cooldown` | `EnemyWeapon.updateAttack` |
| Joueur | `timer` de chaque `Ability` | `UML.md §9` |

Le futur **boss** introduira un vrai pattern State (`BossPhase`) parce qu'il aura plus de phases et de transitions
(voir [`BOSS.md`](BOSS.md)).

### 3.7 Utility class (non-instanciable)

Fonctions pures sans état, constructeur privé : `Collision`, `Pathfinder`, `EnemyVision`, `PlayerInput`, `EnemyAnimations`,
`SkullKnightAnimations`, `ShadowRenderer`, `DebugRenderer`. Rend ces briques **testables sans libGDX graphique**.

### 3.8 Singleton « par statique » — `GameSettings`

Les réglages sont globaux, donc statiques (`GameSettings.pressed(Action.DASH)`), chargés une fois par `Main.create()`.
Choix de simplicité : l'accès n'est pas injecté. Limite connue : difficile à tester isolément (dépend de `Gdx.input`).
Une évolution propre serait une interface `InputSettings` injectée dans `Player`.

### 3.9 Algorithme : A\* — `Pathfinder.findPath`

Recherche de chemin sur la grille de tuiles : 8 directions, heuristique octile, coût diagonal √2 ≈ 1,4142,
interdiction de couper les coins de mur, `MAX_EXPANSIONS = 6000` (borne le coût par appel), puis **lissage** du chemin
(on saute au point le plus lointain encore en ligne droite, `smooth`). `EnemyAI` ne recalcule que toutes les 0,4 s
ou si la cible a bougé de plus de 8 px (`repathTimer`).

### 3.10 Autres idiomes

| Idiome | Où |
|---|---|
| **Cycle de vie / RAII manuel** (`Disposable`) | `Player`, `GameRenderer`, `MapLoader`, `Theme`… libération explicite des textures |
| **Mouvement par pas** | `Character.move` découpe le déplacement en pas ≤ 4 px et résout x puis y → pas de traversée de mur, glisse le long des murs |
| **Séparation temps réel / temps de jeu** | `GameScreen.update` calcule `live` et `worldDt` ; les `ShapeEffect` utilisent le `delta` réel |
| **Données dans la carte** | les points d'apparition sont dans l'éditeur Tiled (calque `Spawns`), pas en dur dans le code |

## 4. Choix discutables et pistes d'amélioration (honnêteté)

| Constat | Risque | Piste |
|---|---|---|
| `GameScreen.spawnEnemies()` fait des `new ShooterEnemy / new SkullKnight` | ajouter un type ou un boss = modifier cette méthode (viole O) | **Factory** `EnemyFactory` pilotée par le nom des objets Tiled (voir `BOSS.md`) |
| `World` a des champs publics | état modifiable de partout | accesseurs, ou garder tel quel (contexte interne) |
| `EnemyAnimations.draw(ShooterEnemy, …)` est typé sur le tireur | un 3ᵉ ennemi demande une 3ᵉ classe d'animations | interface `EnemyRenderer` (Strategy) si le nombre d'ennemis grandit |
| `GameSettings` statique | tests difficiles | injection par interface |
| `Enemy` mélange logique et affichage de debug (`drawVision`, `drawAlert`) | couplage logique/rendu | déplacer vers un `EnemyDebugRenderer` |
| `sourceCompatibility = 8` mais `List.of` (Java 9) est utilisé dans `World` | ne compilerait pas sur un vrai JDK 8 | passer à `17` dans `build.gradle` ou remplacer par `Arrays.asList` |
