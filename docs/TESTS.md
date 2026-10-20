# Tests

Le sujet demande « une couverture de tests pour garantir la conformité des morceaux de code ajoutés ».
Ce document décrit ce qui est testé, comment les lancer, et **ce qui diffère du plan initial ou va au-delà** (§5).

## 1. Lancer les tests

```bash
./gradlew core:test                     # exécute les tests
./gradlew core:test core:jacocoTestReport   # + rapport de couverture
```

- Rapport des tests : `core/build/reports/tests/test/index.html`
- Rapport de couverture : `core/build/reports/jacoco/test/html/index.html`
- Technologie : **JUnit 5** (5.10.2) + **JaCoCo**, configurés dans `core/build.gradle`. Aucun test ne demande de fenêtre ni d'OpenGL.

## 2. État actuel

**89 tests, 0 échec.** Couverture de lignes de **toute** la base : ≈ 15 % — normal, car le rendu, les menus et l'UI
(`render`, `ui`, `screen/*Screen`, `camera`, `settings`) ne sont pas testés automatiquement (voir §4).
Sur la **logique pure** (ce que le sujet cherche à protéger) :

| Classe | Couverture de lignes | Fichier de test |
|---|---|---|
| `Collision` | 100 % | `world/CollisionTest` |
| `Pathfinder` (A\*) | 99 % | `world/PathfinderTest` |
| `World` | 100 % | `world/WorldTest` |
| `Door` | 91 % | `world/DoorTest` |
| `Entity` | 100 % | `entity/CharacterTest` |
| `Character` | 96 % | `entity/CharacterTest` |
| `Direction` | 100 % | `entity/DirectionTest` |
| `Bullet` | 64 % (le reste = dessin) | `entity/projectile/BulletTest` |
| `Stun` | 100 % | `entity/enemy/StunTest` |
| `Enemy` | 46 % (le reste = dessin) | `entity/enemy/EnemyTest` |
| `EnemyAI` | 43 % (patrouille et poursuite non testées) | `entity/enemy/EnemyAITest` |
| `Ability` / `DashAbility` | 70 % / 94 % | `entity/player/DashAbilityTest` |
| `Corpse` | 100 % | `entity/player/CorpseTest` |
| `HitStop`, `SlowMotion`, `DeathSequence` | 100 % | `screen/*Test` |

## 3. Ce que chaque test vérifie

| Fichier | Comportements |
|---|---|
| `CollisionTest` | case vide / mur / hors carte ; ligne de vue libre ou bloquée ; rectangle dans le vide, sur un mur, qui dépasse ; aucun calque |
| `PathfinderTest` | trajet libre → liste vide ; mur → chemin qui contourne et finit sur la cible ; **chaque segment du chemin est libre** ; mur sans passage → `null` |
| `WorldTest` | `allEnemiesDead` ; `makeNoise` alerte les ennemis à portée, pas ceux trop loin, pas la source |
| `DoorTest` | getters, `requiresClear`, `isTouchedBy` |
| `CharacterTest` | un coup = mort, une seule mort même si frappé deux fois, invincible, `respawn`, déplacement libre, **mur bloque**, **glisse le long du mur**, visée → direction, centrage des deux hitbox |
| `DirectionTest` | axes dominants, vecteur nul, égalité |
| `BulletTest` | vitesse, direction normalisée, expiration à 3 s, mort sur un mur, `reflect` (×1,6, change de camp, remet la durée de vie à zéro), `kill`, hitbox |
| `StunTest` | inactif au départ, recule dans la direction du choc, **en ralentissant**, s'arrête après `TIME` |
| `EnemyTest` | un coup tue ; `parried` : sonne, **se retourne vers le joueur**, alerte ; sans effet sur un mort ; se reprend après la stupeur ; `hearNoise` |
| `EnemyAITest` | alerte quand il voit, mémorise la position, la perd après 1,5 s, bruit ignoré si le joueur est vu, `alertAt` |
| `DashAbilityTest` | prêt/actif/recharge ; vitesse 460 ; direction ; nouveau dash ignoré pendant la recharge ; `pollStart` une seule fois ; `reset` |
| `CorpseTest` | tombe du côté opposé au tir, glisse en ralentissant, temps, `reset` |
| `HitStopTest` | gel du parry (3 images de 0,04 s) vs coup (2) ; ralenti 0,35 s à ×0,3 ; un coup ne raccourcit pas un parry ; `reset` |
| `SlowMotionTest` | échelles monde ×0,2 / joueur ×0,6 ; la jauge se vide, se recharge ; **ne peut pas redémarrer sous 0,3** ; `reset` |
| `DeathSequenceTest` | gel à l'impact, puis monde à ×0,25 et effets en temps réel ; game over à 0,45 s ; `reset` |

Chaque test suit la convention `situation_résultatAttendu` et ne dépend d'aucun `sleep` : les classes de temps prennent un `delta` qu'on fait avancer à la main.

## 4. Ce qui n'est pas testé automatiquement, et pourquoi

| Zone | Raison | Alternative |
|---|---|---|
| `Player`, `MeleeAttack`, `ParryAbility` | `Player` charge des textures (`PlayerAnimations`) → exige OpenGL ; les deux capacités appellent `Player` et `World` | checklist manuelle (§6) ; piste : extraire une interface `PlayerRenderer` |
| `ShooterEnemy`, `SkullKnight`, `EnemyWeapon` | `ShooterEnemy` demande une texture de laser (OpenGL) ; le chevalier appelle `Player.hit` | checklist manuelle ; piste : injecter la texture |
| `BulletSystem` | a besoin d'un `Player` | idem |
| `MapLoader`, `GameScreen` (portes, étages) | lisent un `.tmx` et créent des ressources GL | checklist manuelle |
| `render`, `ui`, menus, `GameCamera`, `GameSettings` | dessin, fenêtre, `Gdx.input` statique | checklist manuelle ; piste : interface `InputSettings` injectée |
| `EnemyAI.patrol` / `chaseLastKnown` | dépendent du hasard (`MathUtils.random`) et de plusieurs classes ; non couverts pour l'instant | à ajouter avec un générateur injectable |

## 5. Écarts avec le plan initial, et tests « en plus »

Le premier `TESTS.md` (avant de coder les tests) proposait un plan. Voici ce qui a changé ou dépasse le strict nécessaire :

**Différent du plan**

- **Pas de Mockito.** Le plan prévoyait de « mocker » `Player` et `Gdx.input`. J'ai préféré passer `null` comme `Player` à `DashAbility` (ça marche car `steer`/`trigger` ne s'en servent pas) et ne pas tester ce qui exige un vrai `Player`. Conséquence : `MeleeAttack` et `ParryAbility` ne sont pas testées (§4).
- **JUnit sans `MockitoExtension`** : une seule dépendance de test ajoutée (`junit-jupiter`) plus `junit-platform-launcher` (requis par Gradle 9).
- **Couverture globale faible (≈ 15 %)** alors que le plan ne chiffrait rien : elle est dominée par le code de rendu non testable.

**Plus que le minimum** (utile mais pas strictement demandé)

- Deux classes d'aide dans `src/test` : `TestMaps` (construit une carte à partir de texte) et `TestEnemy` (ennemi qui ne fait rien).
- Tests de `Door`, `World`, `Corpse`, `Stun` et `DeathSequence`, qui n'étaient pas en priorité 1 dans le plan.
- Test **de propriété** dans `PathfinderTest` (« chaque segment est libre »), plus fort qu'un simple « le chemin existe ».
- **JaCoCo** configuré avec rapports HTML et XML.

**Ajouts au code de production : aucun.** Seuls `core/build.gradle` (dépendances, JaCoCo) et le dossier `core/src/test` ont changé.

## 6. Tests manuels (pour ce qui n'est pas automatisé)

Liste de contrôle à rejouer avant chaque livraison :

- [ ] Le jeu se lance, le menu s'affiche, JOUER charge l'étage.
- [ ] Déplacements ZQSD, collisions avec murs/meubles, pas de traversée en dash.
- [ ] Dash invincible : un laser tiré pendant le dash ne tue pas.
- [ ] Clic gauche : un garde touché meurt, éclat blanc + arrêt sur image.
- [ ] Clic droit : un laser parré repart et tue un garde ; un chevalier parré est repoussé et sonné, même touché dans le dos.
- [ ] Les gardes accourent au bruit d'un kill/tir, contournent les murs (A\*).
- [ ] Mort : séquence ralentie → Game Over ; `R` / `Entrée` relance, `Échap` va au menu.
- [ ] Portes : verrouillées (cadenas) tant qu'un garde vit, ouvertes ensuite ; fondu au noir ; on arrive à la bonne entrée de l'étage suivant ; `R` recommence l'étage courant.
- [ ] Pause : image figée, Reprendre / Recommencer / Paramètres / Menu fonctionnent.
- [ ] Paramètres : remappage d'une touche → persiste après relance ; plein écran.
- [ ] Redimensionner la fenêtre : le rendu reste net, ratio préservé.
