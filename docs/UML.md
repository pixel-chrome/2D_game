# Diagrammes UML

Les diagrammes sont écrits en **Mermaid** : ils s'affichent directement sur GitHub, dans VS Code
(extension « Markdown Preview Mermaid Support ») et dans IntelliJ. Pour exporter une image :
coller le bloc sur <https://mermaid.live>.

Légende : `<<abstract>>` classe abstraite · `<<interface>>` interface · `<<enumeration>>` enum ·
`-->` association · `*--` composition (le tout possède la partie) · `o--` agrégation ·
`<|--` héritage · `<|..` implémentation · `..>` dépendance.
Visibilité : `+` public · `#` protégé · `-` privé · `~` package-private.

Sommaire

1. [Vue d'ensemble des packages](#1-vue-densemble-des-packages)
2. [Hiérarchie des entités](#2-hiérarchie-des-entités)
3. [Joueur et capacités](#3-joueur-et-capacités)
4. [Ennemis](#4-ennemis)
5. [Monde, projectiles, collisions](#5-monde-projectiles-collisions)
6. [Écrans et gestion du temps](#6-écrans-et-gestion-du-temps)
7. [Rendu et effets](#7-rendu-et-effets)
8. [Interface et réglages](#8-interface-et-réglages)
9. [Diagramme d'états : IA ennemie](#9-diagramme-détats--ia-ennemie)
10. [Diagrammes de séquence](#10-diagrammes-de-séquence)
11. [Extension prévue : boss final](#11-extension-prévue--boss-final)

---

## 1. Vue d'ensemble des packages

Base : `fr.epitech.katanagame`. La flèche va de « utilise » vers « utilisé ».

```mermaid
flowchart LR
    Launcher["lwjgl3<br/>Lwjgl3Launcher"] --> Main

    subgraph core["core : fr.epitech.katanagame"]
        Main["Main (Game)"]
        screen["screen<br/>GameScreen, menus,<br/>HitStop, SlowMotion, DeathSequence"]
        entity["entity<br/>Entity, Character, Direction"]
        player["entity.player<br/>Player, Ability, Dash, Melee, Parry"]
        enemy["entity.enemy<br/>Enemy, ShooterEnemy, SkullKnight,<br/>EnemyAI, EnemyVision, EnemyWeapon, Stun"]
        projectile["entity.projectile<br/>Bullet, LaserAssets"]
        world["world<br/>World, MapLoader, Collision,<br/>Pathfinder, BulletSystem"]
        render["render<br/>GameRenderer, ShapeEffect,<br/>Blood/Parry/Slash/SlowMo, Shadow, Debug"]
        ui["ui<br/>Theme, FilmOverlay,<br/>MenuBackground, Crosshair"]
        settings["settings<br/>GameSettings"]
        camera["camera<br/>GameCamera"]
    end

    Main --> screen
    Main --> ui
    Main --> settings
    screen --> world
    screen --> render
    screen --> camera
    screen --> player
    screen --> enemy
    player --> entity
    enemy --> entity
    enemy --> projectile
    player --> world
    enemy --> world
    world --> entity
    world --> render
    render --> world
    projectile --> world
    player --> settings
    ui --> settings
```

Deux cycles volontaires : `world ↔ render` (le `World` possède les effets visuels, le `GameRenderer` lit le `World`)
et `entity.* → world` (les entités reçoivent le `World` en paramètre de `update`). Le `World` joue le rôle de **façade**
du contexte de jeu (voir `PATTERNS.md`).

---

## 2. Hiérarchie des entités

```mermaid
classDiagram
    direction TB

    class Entity {
        <<abstract>>
        #Vector2 position
        -Rectangle bounds
        -Rectangle feet
        +update(delta, world)* void
        +draw(batch)* void
        +getPosition() Vector2
        +getBounds() Rectangle
        +getFeetBounds() Rectangle
        #collidesWithSolid(solids) boolean
    }

    class Character {
        <<abstract>>
        #Vector2 aim
        -boolean alive
        +isAlive() boolean
        +isInvincible() boolean
        +takeDamage() void
        +respawn(x, y) void
        #onDeath() void
        +getAim() Vector2
        +getFacing() Direction
        #move(dx, dy, solids) boolean
    }

    class Direction {
        <<enumeration>>
        UP
        DOWN
        LEFT
        RIGHT
        +of(Vector2)$ Direction
    }

    class Player
    class Enemy {
        <<abstract>>
    }
    class ShooterEnemy
    class SkullKnight

    Entity <|-- Character
    Character <|-- Player
    Character <|-- Enemy
    Enemy <|-- ShooterEnemy
    Enemy <|-- SkullKnight
    Character ..> Direction : getFacing()
```

- **Deux hitbox par entité** : `bounds` (corps entier → dégâts) et `feet` (pieds → collisions murs). C'est ce qui permet
  de passer « derrière » un mur en vue top-down.
- **Un coup = mort** est décidé une seule fois, dans `Character.takeDamage()`. `isInvincible()` (dash) et `onDeath()`
  sont les points d'extension.
- `Bullet` n'est **pas** une `Entity` : un projectile n'a ni pieds ni état vivant/mort au sens de `Character`.

---

## 3. Joueur et capacités

```mermaid
classDiagram
    direction LR

    class Player {
        -float stateTime
        -float flash
        -boolean moving
        +isInvincible() boolean
        +hit(dir) void
        +update(delta, world) void
        +draw(batch) void
        +pollDashStart() boolean
        +pollParrySuccess() boolean
        +pollAttackHit() boolean
        +isParrying() boolean
        +isDashing() boolean
        +getAttackBounds() Rectangle
        ~flash() void
        -steering(world) Vector2
    }

    class Ability {
        <<abstract>>
        #Player player
        #float timer
        #float cooldown
        ~tick(delta) void
        ~isActive() boolean
        ~isReady() boolean
        ~remaining() float
        ~steer(world) Vector2
        ~act(world) void
        ~reset() void
    }

    class DashAbility {
        -Direction facing
        ~trigger(direction) void
        ~steer(world) Vector2
        ~pollStart() boolean
    }

    class MeleeAttack {
        -Rectangle bounds
        -float lungeTimer
        ~trigger(world) void
        ~cancel() void
        ~steer(world) Vector2
        -strike(world) void
        ~pollHit() boolean
    }

    class ParryAbility {
        -float animTimer
        ~trigger(world) boolean
        ~steer(world) Vector2
        ~act(world) void
        ~pollSuccess() boolean
    }

    class PlayerInput {
        <<utility>>
        ~readMove()$ Vector2
        ~updateAim(player, world)$ void
    }

    class Corpse {
        ~fall(dir) void
        ~advance(delta) Vector2
    }

    class PlayerAnimations {
        ~draw(...) void
    }

    class DashEffects {
        ~update(...) void
        ~draw(...) void
    }

    Player *-- DashAbility
    Player *-- MeleeAttack
    Player *-- ParryAbility
    Player *-- Corpse
    Player *-- PlayerAnimations
    PlayerAnimations *-- DashEffects
    Player ..> PlayerInput : lit les commandes
    Ability <|-- DashAbility
    Ability <|-- MeleeAttack
    Ability <|-- ParryAbility
    Ability --> Player : player
    Player o-- Ability : abilities[] (ordre de priorité)
```

Principe : `Player` **compose** ses capacités au lieu de tout contenir. Chaque tour, `Player.update` :

1. fait avancer les minuteurs (`Ability.tick`),
2. demande à la première capacité qui le souhaite d'**imposer un déplacement** (`Ability.steer`) — priorité dash > coup > parry,
3. applique l'effet continu du parry (`ParryAbility.act`).

Les `poll…()` sont des **drapeaux à lecture unique** (ils renvoient vrai une fois puis se remettent à faux) : c'est ainsi que
`GameScreen` apprend qu'un dash a démarré ou qu'un parry a réussi, sans que `Player` connaisse la caméra ni le `HitStop`.

---

## 4. Ennemis

```mermaid
classDiagram
    direction TB

    class Enemy {
        <<abstract>>
        #EnemyAI ai
        -Stun stun
        -float stateTime
        -float deathTime
        +update(delta, world) void
        #think(delta, world)* void
        #tick(delta) void
        #onDeadTick() void
        #onStunned() void
        #onParried() void
        +parried(dir, from) void
        +isStunned() boolean
        +hearNoise(at) void
        +drawVision(shapes, walls) void
        +drawCharge(shapes) void
        +drawAlert(shapes) void
        +draw(batch)* void
        ~moveForAI(dx, dy, walls) boolean
        +loadAssets()$ void
        +disposeAssets()$ void
    }

    class ShooterEnemy {
        -EnemyWeapon weapon
        -Texture laserTexture
        #think(delta, world) void
        +draw(batch) void
        +drawCharge(shapes) void
    }

    class SkullKnight {
        -float attackTime
        -float cooldown
        #think(delta, world) void
        -updateAttack(...) void
        -strike(...) void
        +draw(batch) void
    }

    class EnemyAI {
        <<package>>
        -boolean alerted
        -boolean seesPlayer
        -Vector2 lastKnown
        -List~Vector2~ path
        ~updateAlert(sees, target, delta) void
        ~patrol(delta, walls) void
        ~chaseLastKnown(delta, walls) void
        ~alertAt(at) void
        ~hearNoise(at) void
    }

    class EnemyVision {
        <<utility>>
        ~canSee(enemy, target, sight)$ boolean
        ~draw(enemy, shapes, walls)$ void
    }

    class EnemyWeapon {
        <<package>>
        ~updateAttack(delta, target, world) void
        ~updateMotion(delta, walls) void
        ~stagger(time) void
        ~stopAttacking() void
    }

    class Stun {
        <<package>>
        ~start(dir) void
        ~advance(delta, enemy, walls) boolean
        ~isActive() boolean
    }

    class EnemyAnimations {
        <<utility>>
        ~draw(ShooterEnemy, batch)$ void
    }
    class SkullKnightAnimations {
        <<utility>>
        ~draw(SkullKnight, batch, r, g, b)$ void
    }
    class DirectionalAnimation {
        +sideRight(...)$ DirectionalAnimation
        +sideLeft(...)$ DirectionalAnimation
        +withFrameTime(t) DirectionalAnimation
        +get(Direction) Animation
    }

    Enemy <|-- ShooterEnemy
    Enemy <|-- SkullKnight
    Enemy *-- EnemyAI
    Enemy *-- Stun
    ShooterEnemy *-- EnemyWeapon
    EnemyAI --> Enemy : enemy (déplacement, position)
    EnemyWeapon --> ShooterEnemy : enemy
    Enemy ..> EnemyVision : drawVision
    ShooterEnemy ..> EnemyVision : canSee
    SkullKnight ..> EnemyVision : canSee
    ShooterEnemy ..> EnemyAnimations : draw
    SkullKnight ..> SkullKnightAnimations : draw
    EnemyAnimations ..> DirectionalAnimation
    SkullKnightAnimations ..> DirectionalAnimation
```

**Qui fait quoi ?** `Enemy` possède le cycle de vie commun et délègue :

| Préoccupation | Classe |
|---|---|
| Perception (voir, alerte, mémoire) | `EnemyVision` + `EnemyAI.updateAlert` |
| Déplacement (patrouille, poursuite A\*) | `EnemyAI` + `Pathfinder` |
| Réaction au parry | `Stun` + hook `onParried()` |
| Attaque propre au type | sous-classe (`think`) + `EnemyWeapon` pour le tireur |
| Rendu | `*Animations` (classes utilitaires, pas de logique de jeu) |

Le **cycle `update`** de `Enemy` est une *Template Method* : l'ordre `stateTime → mort ? → tick → sonné ? → think` est figé
dans `Enemy`, les sous-classes ne remplissent que `think` et les hooks `tick/onDeadTick/onStunned/onParried`.

---

## 5. Monde, projectiles, collisions

```mermaid
classDiagram
    direction LR

    class World {
        +Viewport viewport
        +TiledMapTileLayer[] moveLayers
        +TiledMapTileLayer[] bulletLayers
        +TiledMapTileLayer[] sightLayers
        +Player player
        +List~Enemy~ enemies
        +List~Bullet~ bullets
        +BloodEffects blood
        +ParryEffects parryFx
        +SlashEffects slashFx
        +List~ShapeEffect~ effects
        +makeNoise(at, radius, source) void
    }

    class MapLoader {
        -TiledMap map
        -Vector2 playerSpawn
        -List~Vector2~ enemySpawns
        -List~Vector2~ knightSpawns
        +getMoveLayers() TiledMapTileLayer[]
        +getBulletLayers() TiledMapTileLayer[]
        +getSightLayers() TiledMapTileLayer[]
        +getPlayerSpawn() Vector2
        +getEnemySpawns() List
        +getKnightSpawns() List
        +dispose() void
    }

    class Collision {
        <<utility>>
        +isSolid(layers, px, py)$ boolean
        +hasLineOfSight(layers, from, to)$ boolean
        +overlapsSolid(layers, rect)$ boolean
    }

    class Pathfinder {
        <<utility>>
        +isClear(layers, from, to)$ boolean
        +findPath(layers, from, to)$ List~Vector2~
    }

    class BulletSystem {
        -World world
        -Runnable onPlayerKilled
        +update(delta) void
        -hitPlayer(bullet) void
        -hitEnemies(bullet) void
    }

    class Bullet {
        +float SPEED$
        -Vector2 position
        -Vector2 velocity
        -boolean reflected
        +update(delta, walls) void
        +draw(batch) void
        +reflect(direction) void
        +isReflected() boolean
        +kill() void
    }

    class LaserAssets {
        <<utility>>
        +loadAssets()$ void
        +getRandomTexture()$ Texture
    }

    World o-- Player
    World o-- Enemy
    World o-- Bullet
    MapLoader ..> World : fournit les calques
    BulletSystem --> World
    BulletSystem ..> Bullet
    Bullet ..> Collision
    Pathfinder ..> Collision
    EnemyAI ..> Pathfinder
    Entity ..> Collision
    Bullet ..> LaserAssets : texture
```

`World` est volontairement **passif** : il regroupe l'état partagé (calques, joueur, ennemis, balles, effets) pour éviter de
passer cinq paramètres à chaque `update`. Les trois tableaux de calques correspondent à trois questions différentes :
« qui bloque mon corps ? » (`moveLayers`), « qui bloque une balle ? » (`bulletLayers`), « qui bloque ma vue ? » (`sightLayers`).

---

## 6. Écrans et gestion du temps

```mermaid
classDiagram
    direction TB

    class Game {
        <<libGDX>>
    }
    class ScreenAdapter {
        <<libGDX>>
    }

    class Main {
        +String TITLE$
        +Theme theme
        +FilmOverlay film
        +MenuBackground background
        +create() void
        +switchTo(Screen) void
        +dispose() void
    }

    class GameScreen {
        -MapLoader mapLoader
        -GameCamera camera
        -Player player
        -World world
        -GameRenderer renderer
        -BulletSystem bullets
        -boolean debug
        +restart() void
        +render(delta) void
        +drawFrame() void
        +updateEffects(delta) void
        -update(delta) void
        -spawnEnemies() void
    }

    class MainMenuScreen
    class PauseScreen
    class GameOverScreen
    class SettingsScreen

    class HitStop {
        <<package>>
        ~onParry() void
        ~onAttackHit() void
        ~isFrozen(delta) boolean
        ~worldScale(delta) float
    }
    class SlowMotion {
        <<package>>
        ~update(delta, wantsSlow) void
        ~worldScale() float
        ~playerScale() float
        ~getCharge() float
    }
    class DeathSequence {
        <<package>>
        ~start() void
        ~advance(delta) void
        ~effectsScale() float
        ~worldScale() float
        ~isOver() boolean
    }

    Game <|-- Main
    ScreenAdapter <|-- GameScreen
    ScreenAdapter <|-- MainMenuScreen
    ScreenAdapter <|-- PauseScreen
    ScreenAdapter <|-- GameOverScreen
    ScreenAdapter <|-- SettingsScreen

    Main --> GameScreen : switchTo
    Main --> MainMenuScreen : create
    GameScreen *-- HitStop
    GameScreen *-- SlowMotion
    GameScreen *-- DeathSequence
    GameScreen *-- World
    GameScreen *-- BulletSystem
    GameScreen *-- GameRenderer
    GameScreen *-- GameCamera
    PauseScreen --> GameScreen : level (dessine l'image figée)
    GameOverScreen --> GameScreen : level (restart, updateEffects)
    SettingsScreen --> GameScreen : backdrop (optionnel)
```

**Gestion du temps** : trois sources d'échelle de temps se combinent dans `GameScreen.update`.

| Source | Effet sur le joueur (`live`) | Effet sur le monde (`worldDt`) |
|---|---|---|
| `SlowMotion` (touche maintenue) | ×0,6 | ×0,2 |
| `DeathSequence` (joueur mort) | effets figés 0,06 s | ×0,25 |
| `HitStop` (coup / parry) | tout figé 0,06–0,09 s | puis ×0,3 pendant 0,35 s (parry) |

Les effets visuels (`ShapeEffect`) avancent au **temps réel** (`delta`), pour continuer à s'animer pendant un arrêt sur image.

---

## 7. Rendu et effets

```mermaid
classDiagram
    direction LR

    class ShapeEffect {
        <<interface>>
        +update(delta) void
        +draw(shapes) void
        +clear() void
    }

    class SlashEffects {
        +speedLine(at, dir) void
        +hit(at, dir) void
    }
    class ParryEffects {
        +swing(at, angleDeg) void
        +impact(at, dir) void
        +drawFlash(shapes, camera) void
    }
    class BloodEffects {
        +burst(at, dir, walls) void
        +splatter(at, dir, walls) void
        +update(delta, walls) void
        +draw(shapes) void
        +drawFlash(shapes, camera) void
        +clear() void
    }

    class GameRenderer {
        -SpriteBatch batch
        -ShapeRenderer shapes
        -SlowMoEffects slowMoEffects
        +render(world, player, viewport, debug, charge, intensity) void
        +dispose() void
    }
    class SlowMoEffects {
        ~drawBar(...) void
        ~drawOverlay(intensity) void
    }
    class ShadowRenderer {
        <<utility>>
        +initialize()$ void
        +dispose()$ void
    }
    class DebugRenderer {
        <<utility>>
        +draw(shapes, world, player)$ void
    }

    ShapeEffect <|.. SlashEffects
    ShapeEffect <|.. ParryEffects
    GameRenderer *-- SlowMoEffects
    GameRenderer ..> World : lit
    GameRenderer ..> DebugRenderer
    GameRenderer ..> ShapeEffect : pour chaque effet
    World *-- BloodEffects
    World *-- SlashEffects
    World *-- ParryEffects
```

**Ordre de dessin** dans `GameRenderer.render` (du dessous au dessus) :

1. carte Tiled → 2. sang au sol → 3. cônes de vision → 4. points de charge des tireurs →
5. sprites (ennemis puis joueur) → 6. lasers → 7. indicateurs d'alerte + `ShapeEffect` + flashs →
8. jauge et voile de ralenti → 9. hitbox de debug (si `F1`).

---

## 8. Interface et réglages

```mermaid
classDiagram
    direction LR

    class Theme {
        +titleStyle
        +hintStyle
        +button(text, centered) TextButton
        +spaced(String)$ String
        +dispose() void
    }
    class FilmOverlay {
        +draw(delta, projection, dim, vignetteAlpha) void
    }
    class MenuBackground {
        +render(delta, projection) void
    }
    class Crosshair {
        +apply() void
        +restore()$ void
    }
    class GameSettings {
        <<static>>
        +float musicVolume$
        +float sfxVolume$
        +boolean fullscreen$
        +load()$ void
        +save()$ void
        +pressed(Action)$ boolean
        +justPressed(Action)$ boolean
        +setKey(Action, int)$ void
    }
    class Action {
        <<enumeration>>
        UP
        DOWN
        LEFT
        RIGHT
        DASH
        SLOWMO
    }
    class GameCamera {
        +centerOn(position) void
        +shake(amount) void
        +update(delta, playerPos, mapW, mapH) void
        +resize(w, h) void
    }

    MenuBackground --> FilmOverlay
    GameSettings +-- Action
    Main --> Theme
    Main --> FilmOverlay
    Main --> MenuBackground
    GameScreen --> Crosshair
    GameScreen --> GameCamera
    PlayerInput ..> GameSettings
```

---

## 9. Diagramme d'états : IA ennemie

Vue d'un ennemi (le tireur et le chevalier partagent ces états ; seule l'attaque diffère).

```mermaid
stateDiagram-v2
    [*] --> Pause
    Pause --> Marche : délai écoulé / point de patrouille choisi
    Marche --> Pause : arrivé / bloqué
    Pause --> Alerte : voit le joueur / entend un bruit
    Marche --> Alerte : voit le joueur / entend un bruit

    Alerte --> Poursuite : cible hors de portée d'attaque
    Poursuite --> Attaque : voit le joueur à portée et la ligne est libre
    Attaque --> Poursuite : joueur sorti de portée
    Poursuite --> Alerte : arrivé au dernier endroit connu
    Alerte --> Pause : 1,5 s sans voir le joueur

    Alerte --> Sonné : parry
    Poursuite --> Sonné : parry
    Attaque --> Sonné : parry (attaque annulée)
    Pause --> Sonné : parry
    Marche --> Sonné : parry
    Sonné --> Alerte : 0,7 s écoulées (tourné vers le joueur)

    Pause --> Mort : touché
    Marche --> Mort : touché
    Alerte --> Mort : touché
    Poursuite --> Mort : touché
    Attaque --> Mort : touché
    Sonné --> Mort : touché
    Mort --> [*]
```

États réellement stockés : `alerted`, `walking`, `chasing` (dans `EnemyAI`), `Stun.timer`, `isAlive()` (dans `Character`).
Le diagramme est la lecture logique de ces drapeaux.

Machine à états du **joueur** (pendant le `Dash` il est invincible : un tir ou un coup ne le fait pas passer à `Mort`) :

```mermaid
stateDiagram-v2
    [*] --> Libre
    Libre --> Dash : Espace (prêt)
    Libre --> Coup : clic gauche (prêt)
    Libre --> Parry : clic droit (prêt, pas en dash)
    Dash --> Libre : 0,12 s
    Coup --> Libre : 0,56 s
    Coup --> Parry : clic droit (annule le coup)
    Parry --> Libre : 0,22 s (0,32 s d'animation)
    Libre --> Mort : touché
    Coup --> Mort : touché
    Parry --> Mort : touché
    Mort --> Libre : R / Recommencer
```

---

## 10. Diagrammes de séquence

### 10.1 Une image de jeu (`GameScreen.render`)

```mermaid
sequenceDiagram
    participant L as libGDX
    participant G as GameScreen
    participant P as Player
    participant E as Enemy (×N)
    participant B as BulletSystem
    participant C as GameCamera
    participant R as GameRenderer

    L->>G: render(delta)
    alt Échap
        G->>L: setScreen(PauseScreen)
    else
        G->>G: update(delta)
        G->>G: SlowMotion / DeathSequence / HitStop → live, worldDt
        G->>P: update(live, world)
        G->>P: pollDashStart / pollParrySuccess / pollAttackHit
        P-->>G: drapeaux (secousse caméra, hit stop)
        opt worldDt > 0
            loop chaque ennemi
                G->>E: update(worldDt, world)
            end
            G->>B: update(worldDt)
        end
        G->>C: update(delta, playerPos, mapW, mapH)
        alt joueur mort et DeathSequence terminée
            G->>L: setScreen(GameOverScreen)
        else
            G->>R: render(world, player, viewport, debug, ...)
        end
    end
```

### 10.2 Un parry réussi contre un tireur

```mermaid
sequenceDiagram
    actor U as Joueur
    participant P as Player
    participant PA as ParryAbility
    participant W as World
    participant S as ShooterEnemy
    participant Bu as Bullet
    participant BS as BulletSystem
    participant G as GameScreen

    U->>P: clic droit
    P->>PA: trigger(world)
    PA->>W: parryFx.swing(...)
    loop chaque image (0,22 s)
        P->>PA: act(world)
        PA->>Bu: reflect(aim) (balle dans le rayon)
        PA->>W: parryFx.impact(...)
        PA->>S: parried(dir, from) (ennemi dans le rayon)
        S->>S: stun.start() puis onParried() → weapon.stagger()
        PA->>W: makeNoise(from, 120, enemy)
        PA-->>P: succeeded = true, cooldown = 0
    end
    G->>P: pollParrySuccess() → vrai
    G->>G: hitStop.onParry() + camera.shake(4)
    BS->>Bu: update(delta)
    BS->>S: takeDamage() (balle renvoyée touche un ennemi)
    BS->>W: blood.splatter(...)
```

### 10.3 Mort du joueur

```mermaid
sequenceDiagram
    participant Bu as Bullet
    participant BS as BulletSystem
    participant P as Player
    participant G as GameScreen
    participant D as DeathSequence
    participant O as GameOverScreen

    BS->>Bu: update → touche le joueur
    BS->>P: hit(dir) → corpse.fall + takeDamage()
    BS->>G: onPlayerKilled.run() → death.start()
    loop jusqu'à 0,45 s
        G->>D: advance(delta)
        G->>G: monde à ×0,25, sang et corps en temps réel
    end
    G->>O: setScreen(new GameOverScreen)
    O->>G: updateEffects(delta) (le corps glisse encore)
    O->>G: restart() (R / bouton)
```

### 10.4 Poursuite d'un ennemi avec A\*

```mermaid
sequenceDiagram
    participant E as Enemy.think
    participant AI as EnemyAI
    participant PF as Pathfinder
    participant Co as Collision

    E->>AI: updateAlert(sees, target, delta)
    E->>AI: chaseLastKnown(delta, walls)
    AI->>AI: repathTimer ≤ 0 ou cible déplacée de > 8 px ?
    AI->>PF: findPath(walls, feetFrom, feetTo)
    PF->>Co: isClear (ligne droite ?) sinon A* sur la grille
    Co-->>PF: overlapsSolid(...)
    PF-->>AI: liste de points de passage (lissée)
    AI->>E: moveForAI(dx, dy, walls) → Character.move
```

---

## 11. Extension prévue : boss final

Non implémenté (voir [`BOSS.md`](BOSS.md)) : voici où il s'insère sans modifier l'existant.

```mermaid
classDiagram
    direction TB

    class Enemy {
        <<abstract>>
    }
    class Boss {
        <<abstract>>
        -int health
        -int maxHealth
        -BossPhase phase
        +takeDamage() void
        +isInvincible() boolean
        #think(delta, world) void
        #onPhaseChange(from, to) void
    }
    class BossPhase {
        <<interface>>
        +enter(boss, world) void
        +update(boss, delta, world) void
        +exit(boss, world) void
        +isFinished() boolean
    }
    class BossAttack {
        <<interface>>
        +start(boss, world) void
        +update(delta, world) boolean
        +cancel() void
    }
    class FinalBoss
    class BossSprites {
        <<utility>>
    }
    class BossHealthBar {
        +draw(shapes, boss, camera) void
    }
    class EnemyFactory {
        <<interface>>
        +create(name, x, y) Enemy
    }

    Enemy <|-- Boss
    Boss <|-- FinalBoss
    Boss o-- BossPhase : phase courante
    BossPhase o-- BossAttack : enchaîne
    FinalBoss ..> BossSprites
    GameRenderer ..> BossHealthBar
    GameScreen ..> EnemyFactory : remplace spawnEnemies()
    MapLoader ..> EnemyFactory : objets « boss »
```
