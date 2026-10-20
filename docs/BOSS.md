# Préparation du boss final

> **Statut : conception uniquement. Rien n'est implémenté.** Ce document fixe où et comment le boss s'insérera
> dans l'architecture actuelle, pour pouvoir le coder plus tard sans refonte.
> Le contenu de gameplay (nom, attaques, nombre de PV) est une **proposition à valider** : c'est un choix de design.

Le boss se trouve dans la **dernière pièce** de l'étage. Le tuer termine le jeu (écran de victoire).

## 1. Proposition de design

**Nom de travail : le Shogun.** Un duelliste en armure, plus grand que les gardes.

| Élément | Proposition |
|---|---|
| Points de vie | 12 (ou barres de vie à 3 phases de 4) |
| Dégâts reçus | coup de sabre : **1** · balle renvoyée : **2** · coup porté pendant qu'il est assommé par un parry : **3** |
| Tuer le joueur | un coup (règle du jeu inchangée) |
| Invincibilité | brève après chaque dégât (évite qu'un seul coup compte plusieurs fois, voir §3.2) |
| Bruit | ignore les bruits (`hearNoise` ne fait rien) : il n'est jamais « distrait » |

### Phases

| Phase | PV restants | Attaques | Idée |
|---|---|---|---|
| **1 — Évaluation** | 100 % → 66 % | éventail de lasers, estoc en ligne droite annoncé | apprendre les deux patterns |
| **2 — Colère** | 66 % → 33 % | + balayage circulaire parable, invocation de 2 gardes | gérer le monde et les gardes en même temps |
| **3 — Dernier souffle** | 33 % → 0 | salves radiales de lasers, enchaînements estoc → balayage, cadence ↑ | tout ce qu'on a appris, plus vite |

### Attaques (chacune est annoncée avant d'agir, comme le chevalier)

| Attaque | Annonce | Comment la contrer | Parable ? |
|---|---|---|---|
| **Éventail** | point lumineux + teinte | parry renvoie les lasers sur lui (2 dégâts) ; ou dash | oui |
| **Estoc** | ligne rouge au sol 0,6 s, puis charge | dash de côté / à travers | non |
| **Balayage** | teinte rouge 0,4 s | parry → il est **assommé** (fenêtre pour le frapper : 3 dégâts) | oui |
| **Invocation** | animation levée d'épée | tuer les gardes au fil de l'eau | — |
| **Salve radiale** | cercle lumineux | parry/dash entre les rayons | oui |

Règle de lisibilité : toute attaque dangereuse a une **annonce visible** et une **réponse du joueur** qui fonctionne.

## 2. Ce que l'architecture actuelle permet déjà

| Besoin du boss | Existant réutilisable |
|---|---|
| Être un ennemi comme les autres (liste, rendu, parry, balles renvoyées) | `Boss extends Enemy` → entre dans `World.enemies`, `BulletSystem.hitEnemies`, `MeleeAttack.strike`, `ParryAbility.act` sans modification |
| Comportement propre | `Enemy.think(...)` est abstraite ; hooks `tick`, `onParried`, `onStunned`, `onDeadTick` |
| Tirer des lasers | `new Bullet(from, dir, texture)` + `world.bullets.add(...)` (comme `EnemyWeapon.fireVolley`) |
| Réaction au parry | `Enemy.parried(dir, from)` + `Stun` |
| Mouvements avec collisions | `Character.move` (via `Enemy.moveForAI`) |
| Annonces d'attaque | `Enemy.drawCharge(ShapeRenderer)` est déjà un point d'extension de rendu |
| Positionnement depuis l'éditeur | calque d'objets `Spawns` de Tiled (un objet nommé `boss`) |
| Mort du joueur au contact | `Player.hit(dir)` + détection vivant→mort dans `GameScreen.update` |
| Feedback de combat | `HitStop`, `camera.shake`, `BloodEffects` |

## 3. Modifications à prévoir dans le code existant

Petites, rétrocompatibles, et toutes localisées.

### 3.1 `Enemy` — taille de hitbox configurable

`Enemy` passe des constantes privées `WIDTH/HEIGHT/FEET_*` à `Character`. Un boss plus grand a besoin d'autres dimensions.
→ ajouter un **second constructeur protégé** `Enemy(x, y, runSpeed, width, height, feetW, feetH, feetOffsetY)`
et faire déléguer l'actuel dessus. Aucune sous-classe existante ne change.

### 3.2 Points de vie : `Boss.takeDamage()` sans toucher à `Character`

`Character.takeDamage()` tue en un coup. Le boss redéfinit la méthode (elle n'est pas `final`) :

```java
@Override
public void takeDamage() {
    if (!isAlive() || isInvincible()) return;
    if (--health > 0) { onHit(); return; }   // encaisse, enclenche l'invincibilité courte / le changement de phase
    super.takeDamage();                       // dernier PV → mort normale (onDeath, sang, etc.)
}
```

**Piège à anticiper** : `MeleeAttack.steer` rappelle `strike()` **à chaque image de la fente** (≈ 8 images à 60 FPS).
Un ennemi normal meurt à la première, mais un boss perdrait 8 PV en un coup. Solution : `Boss.isInvincible()` renvoie vrai
pendant une courte fenêtre (~0,4 s) après chaque dégât (`hitCooldown`). Cette fenêtre sert aussi de feedback (clignotement).

Autre effet à vérifier : `MeleeAttack.strike` fait `world.makeNoise(...)` seulement si `!enemy.isAlive()` → pas de bruit tant que le boss vit. Voulu.

### 3.3 `MapLoader` et la création des ennemis

`GameScreen.spawnEnemies()` (appelée par `enterLevel` à chaque étage) fait `new ShooterEnemy` / `new SkullKnight` en dur, et `MapLoader` classe tout objet non reconnu comme tireur.
Pour le boss (et tout futur ennemi), introduire une **fabrique** :

```java
interface EnemyFactory { Enemy create(String type, float x, float y); }
// MapLoader : getSpawns() renvoie des SpawnPoint(name, x, y) au lieu de deux listes
// GameScreen.spawnEnemies : pour chaque SpawnPoint → factory.create(name, x, y)
```

→ ajouter un ennemi devient « une classe + une ligne dans la fabrique + un nom d'objet dans Tiled » (principe ouvert/fermé).

### 3.4 `World` — accès direct au boss

Ajouter `public Boss boss;` (null hors salle du boss) pour que `GameRenderer` dessine la barre de vie et que `GameScreen` détecte la victoire
sans parcourir `enemies` avec `instanceof`.

### 3.5 Rendu

- `BossAnimations` (classe utilitaire, comme `SkullKnightAnimations`) : sprites agrandis, ombre via `ShadowRenderer.draw`.
- `BossHealthBar` dessinée dans `GameRenderer.render` (étape `ShapeRenderer` au-dessus des effets), à partir de la caméra.
- `Boss.drawCharge` dessine les annonces (ligne d'estoc, cercle de salve).

### 3.6 Pathfinder

`Pathfinder.BODY_W/H` suppose un corps de la taille d'un garde. Un boss large pourrait passer entre deux meubles trop serrés.
Le plus simple : le boss **n'utilise pas** `chaseLastKnown` ; il se déplace par attaques scriptées dans une arène dégagée.

## 4. Nouvelles briques

```
entity/enemy/boss/
├── Boss.java            abstract extends Enemy : PV, invincibilité courte, phase courante, onPhaseChange
├── FinalBoss.java       le Shogun : liste de phases, attaques, rendu
├── BossPhase.java       interface : enter / update / exit / isFinished
├── BossAttack.java      interface : start / update (→ terminé ?) / cancel
├── attacks/
│   ├── FanAttack.java       éventail de lasers
│   ├── DashStrike.java      estoc annoncé
│   ├── SweepAttack.java     balayage parable
│   ├── SummonAttack.java    invocation de gardes
│   └── RadialBurst.java     salve radiale
├── BossAnimations.java
└── BossHealthBar.java       (dans render/)
world/BossRoom.java      déclencheur d'entrée + portes
screen/VictoryScreen.java
```

### Patterns utilisés

| Pattern | Rôle |
|---|---|
| **State** (`BossPhase`) | une classe par phase ; transitions quand les PV franchissent un seuil |
| **Strategy** (`BossAttack`) | chaque attaque est interchangeable ; une phase choisit l'enchaînement |
| **Template Method** (déjà en place) | `Boss.think` appelle `phase.update(...)` ; `Enemy.update` reste inchangé |
| **Factory** (`EnemyFactory`) | création depuis la carte (§3.3) |

Esquisse :

```java
abstract class Boss extends Enemy {
    private int health;
    private BossPhase phase;
    private float hitCooldown;

    @Override protected void think(float delta, World world) { phase.update(this, delta, world); }
    @Override public boolean isInvincible() { return hitCooldown > 0f; }
    @Override public void hearNoise(Vector2 at) { /* ignore */ }
    @Override public void parried(Vector2 dir, Vector2 from) {
        if (phase.canBeParried()) super.parried(dir, from);   // sinon le boss encaisse l'esquive
    }
}
```

### Diagramme

Voir [`UML.md §11`](UML.md#11-extension-prévue--boss-final).

## 5. La pièce du boss

1. **Déclencheur** : `BossRoom` surveille l'entrée du joueur dans une zone (objet Tiled `boss_trigger`).
2. **Existant** : `boss.tmx` est déjà dans le projet et on y arrive par une porte (`Door`, entrée `entry_from_etage_1`). Le fondu, le verrouillage `requires_clear` et le redémarrage sur l'entrée de l'étage courant (`GameScreen.restart`) sont donc **déjà faits**.
3. **Verrouillage de l'arène** : une couche de tuiles `Porte` incluse dès le départ dans `moveLayers` (et `bulletLayers`), **vide** au départ.
   `BossRoom` y pose des cellules pour fermer la salle (`layer.setCell`) : collisions et rendu suivent sans autre code, car `MapLoader` rend déjà les calques.
4. **Combat** : musique/ambiance (quand l'audio existera), barre de vie.
5. **Victoire** : à la mort du boss, déverrouille, puis `VictoryScreen`.
6. **Échec / recommencer** : `GameScreen.restart()` appelle `enterLevel`, qui recrée les ennemis via `spawnEnemies` ; le boss y sera recréé, et `BossRoom.reset()` rouvrira l'arène.

## 6. Plan d'implémentation conseillé

| Étape | Contenu | Vérification |
|---|---|---|
| 1 | Constructeur protégé d'`Enemy` + `EnemyFactory` + refonte de `spawnEnemies` | le jeu se comporte comme avant (mêmes ennemis) |
| 2 | `Boss` (PV, invincibilité courte) avec un seul `BossPhase` vide | tests unitaires PV/invincibilité ; le boss meurt en N coups |
| 3 | Sprite + `BossAnimations` + barre de vie | rendu correct à toutes les tailles de fenêtre |
| 4 | Attaque 1 (`FanAttack`) puis 2, 3… une par une | chaque attaque est annoncée, esquivable, parable si prévu |
| 5 | Phases et seuils de PV | transitions sans doublon d'attaque |
| 6 | `BossRoom` + portes + `VictoryScreen` | recommencer rouvre la salle |
| 7 | Équilibrage | testé à la main, en regardant un run entier |

## 7. Points ouverts (à trancher par toi)

- Nombre de PV et de phases (proposition : 12 PV, 3 phases).
- Le boss tue-t-il au contact ou seulement via ses attaques ?
- Le boss peut-il être tué en un coup après un parry réussi (style *Katana Zero*) plutôt que par PV ?
- Combien de gardes invoqués, et de quel type ?
- Y a-t-il un checkpoint juste avant le boss (recommencer depuis la porte) ou on recommence l'étage ?
- L'arène est `boss.tmx` (déjà créée, vide pour l'instant). Y a-t-il une porte de sortie après le boss, ou la victoire est-elle immédiate ?
