# Arbres de fonctions (call trees)

Qui appelle quoi, dans l'ordre d'exécution. Les noms sont `Classe.méthode`. Un `?` signale une condition,
un `*` une boucle, `[hook]` un point d'extension redéfini par les sous-classes.
Voir aussi les diagrammes de séquence dans [`UML.md §10`](UML.md#10-diagrammes-de-séquence).

Sommaire

1. [Démarrage](#1-démarrage)
2. [Une image de jeu](#2-une-image-de-jeu)
3. [Le joueur](#3-le-joueur)
4. [Les ennemis](#4-les-ennemis)
5. [Projectiles](#5-projectiles)
6. [Monde, collisions, pathfinding](#6-monde-collisions-pathfinding)
7. [Rendu](#7-rendu)
8. [Mort, pause, redémarrage](#8-mort-pause-redémarrage)
9. [Réglages et menus](#9-réglages-et-menus)
10. [Index des classes](#10-index-des-classes)

---

## 1. Démarrage

```
Lwjgl3Launcher.main
├─ StartupHelper.startNewJvmIfRequired          (macOS / Windows : relance la JVM si besoin)
└─ createApplication
   ├─ new Main()
   └─ getDefaultConfiguration                    (titre, vsync, fenêtre 1280×720)

Main.create                                      (appelé par libGDX une fois le contexte GL prêt)
├─ GameSettings.load                             (volumes, plein écran, touches)
│  └─ GameSettings.applyDisplay
├─ new Theme                                     (polices FreeType, styles de boutons)
├─ new FilmOverlay
├─ new MenuBackground(film)
└─ setScreen(new MainMenuScreen(this))
   └─ MainMenuScreen.show                        (input = Stage, fondu 0,9 s)
```

Clic sur **JOUER** → `Gdx.app.postRunnable` (jamais détruire un `Stage` pendant qu'il traite un clic) →
`Main.switchTo(new GameScreen)` → `setScreen` puis `dispose()` de l'ancien écran.

```
GameScreen.<init>
├─ new GameCamera ; new Player                   (les mêmes pour tous les étages)
├─ Enemy.loadAssets
│  ├─ EnemyAnimations.loadAssets
│  ├─ SkullKnightAnimations.loadAssets
│  └─ LaserAssets.loadAssets
├─ ShadowRenderer.initialize
└─ loadLevel(FIRST_MAP, "player_spawn")           → voir ci-dessous

GameScreen.loadLevel(path, entryName)            (premier étage, puis chaque passage de porte)
├─ new MapLoader(path)
│  ├─ TmxMapLoader.load
│  └─ loadSpawns / loadDoors                      (entrées entry_xxx, gardes, portes)
├─ new MapShadows(calques d'ombre, taille de la carte)     (ombres pré-calculées)
├─ libère l'ancien GameRenderer, MapShadows, MapLoader
├─ new World(...) ; new BulletSystem(world, death::start)
├─ new GameRenderer(renderer de carte, portes, ombres)
└─ enterLevel(entryName)
   ├─ MapLoader.getEntry → Player.respawn
   ├─ spawnEnemies
   │  ├─ new ShooterEnemy(x, y)  *                 (un par objet Spawns sans nom)
   │  │  └─ Enemy.<init> → new EnemyAI ; LaserAssets.getRandomTexture ; new EnemyWeapon
   │  └─ new SkullKnight(x, y)  *
   ├─ camera.centerOn ; DeathSequence/SlowMotion/HitStop.reset ; BloodEffects.clear
   └─ doorsLocked = true                           (le joueur doit d'abord quitter la zone d'arrivée)

GameScreen.updateDoors                            (chaque image, hors fondu)
├─ une Door touche Player.getFeetBounds ? sinon doorsLocked = false
├─ doorsLocked ? return
├─ Door.requiresClear ∧ ¬World.allEnemiesDead ? return        (porte verrouillée)
└─ ScreenFade.start(0,4 s, () -> loadLevel(door.targetMap, door.targetEntry))
   └─ ScreenFade.update : noir complet → exécute loadLevel → rallume
```

---

## 2. Une image de jeu

```
GameScreen.render(delta)
├─ ESC ? → game.setScreen(new PauseScreen(game, this)) ; return
├─ ScreenFade actif ? → la partie est figée (mais dessinée) ; sinon :
├─ GameScreen.update(delta)
│  ├─ SlowMotion.update(delta, wantsSlow)
│  ├─ live = delta × SlowMotion.playerScale ; worldDt = delta × SlowMotion.worldScale
│  ├─ joueur mort ? → DeathSequence.advance ; live/worldDt = ×effectsScale / ×worldScale
│  ├─ HitStop.isFrozen ? → tout à 0  :  worldDt ×= HitStop.worldScale
│  ├─ Player.update(live, world)                                    → §3
│  ├─ Player.pollDashStart        ? → camera.shake(2.5)
│  ├─ Player.pollParrySuccess     ? → HitStop.onParry ; camera.shake(4)
│  ├─ Player.pollAttackHit        ? → HitStop.onAttackHit ; camera.shake(3)
│  ├─ ShapeEffect.update(delta) *                                    (temps réel)
│  ├─ BloodEffects.update(live, bulletLayers)
│  ├─ joueur mort + touche R ? → restart                             → §8
│  ├─ worldDt > 0 ?
│  │  ├─ Enemy.update(worldDt, world) *                              → §4
│  │  ├─ (vivant → mort pendant la boucle) ? → DeathSequence.start   (tué au corps à corps)
│  │  └─ BulletSystem.update(worldDt)                                → §5
│  └─ GameCamera.update(delta, playerPos, mapW, mapH)
├─ joueur mort et DeathSequence.isOver ? → setScreen(new GameOverScreen) ; return
├─ updateDoors                                                       → §1
├─ ScreenFade.update                                                 (peut changer d'étage)
├─ F1 ? → debug = !debug ; F2 ? → godMode (Player.setGodMode)
└─ drawFrame → GameRenderer.render(...)                              → §7
```

---

## 3. Le joueur

```
Player.update(delta, world)
├─ stateTime += delta
├─ mort ?
│  ├─ Corpse.advance(delta)           → Vector2 (glissade freinée)
│  └─ Character.move                   ; return
├─ Ability.tick(delta) *               (DashAbility, MeleeAttack, ParryAbility : minuteurs)
├─ PlayerInput.readMove                (ZQSD via GameSettings.pressed)
├─ PlayerInput.updateAim               (souris → viewport.unproject → aim)
├─ GameSettings.justPressed(DASH) ? → DashAbility.trigger(dir ou aim)
├─ steering(world)                     → première Ability dont steer() ≠ null
│  ├─ DashAbility.steer                (460 px/s pendant 0,12 s)
│  ├─ MeleeAttack.steer                (fente 340 px/s ; SlashEffects.speedLine ; strike)
│  └─ ParryAbility.steer               (fente 260 px/s pendant 0,09 s)
├─ Character.move(velocity × delta)    (ou ZQSD × 200 px/s si aucune capacité n'impose)
│  └─ Entity.collidesWithSolid → Collision.overlapsSolid
├─ clic droit ∧ ¬dash ? → ParryAbility.trigger  → (réussi) MeleeAttack.cancel
├─ ParryAbility.isActive ? → ParryAbility.act                       → §3.2
└─ sinon clic gauche ? → MeleeAttack.trigger                        → §3.1
```

### 3.1 Coup de sabre

```
MeleeAttack.trigger(world)
├─ ¬isReady ? return
├─ timer = 0,56 ; cooldown = 0,5 ; lungeTimer = 0,14 ; lungeDir = aim
└─ strike(world)
   └─ pour chaque Enemy vivant dont getBounds ∩ bounds du coup :
      ├─ Character.takeDamage → Enemy.onDeath (deathTime = 0)
      ├─ SlashEffects.hit(at, dir)
      ├─ hit = true (lu par Player.pollAttackHit) ; Player.flash
      └─ mort ? → BloodEffects.splatter ; World.makeNoise(at, 110, enemy)
```

`MeleeAttack.steer` rappelle `strike` à chaque image de la fente : le sabre touche **tout le long** de l'élan.

### 3.2 Parry

```
ParryAbility.trigger(world)
└─ timer = 0,22 ; animTimer = 0,32 ; cooldown = 0,45 ; ParryEffects.swing

ParryAbility.act(world)                (chaque image pendant la fenêtre)
├─ pour chaque Bullet vivante, non renvoyée, à ≤ 34 px de la zone :
│  ├─ Bullet.reflect(aim)              (×1,6 de vitesse, bascule en « renvoyée »)
│  └─ ParryEffects.impact
├─ pour chaque Enemy vivant et non sonné, à ≤ 44 px :
│  ├─ Enemy.parried(dir, from)         → §4.3
│  ├─ World.makeNoise(from, 120, enemy)
│  └─ ParryEffects.impact
└─ réussi ? → succeeded = true ; cooldown = 0 ; Player.flash
```

### 3.3 Dash

```
DashAbility.trigger(dir)
└─ ¬(isReady ∧ ¬isActive) ? return
   dir ; facing = Direction.of(dir) ; started = true ; timer = 0,12 ; cooldown = 0,75

Character.takeDamage
└─ isInvincible() = DashAbility.isActive()     → le dash ignore tout dégât
```

---

## 4. Les ennemis

```
Enemy.update(delta, world)
├─ stateTime += delta
├─ mort ? → deathTime += delta ; EnemyAI.setSeesPlayer(false) ; onDeadTick [hook] ; return
├─ tick(delta) [hook]                                     (SkullKnight : cooldown)
├─ Stun.advance(delta, this, moveLayers)
│  └─ vrai ? → Enemy.moveForAI → Character.move (recul freiné) ; onStunned [hook] ; return
└─ think(delta, world) [hook abstrait]                   → §4.1 / §4.2
```

### 4.1 Tireur — `ShooterEnemy.think`

```
ShooterEnemy.think
├─ joueur mort ? → EnemyWeapon.stopAttacking ; return
├─ EnemyWeapon.updateTimers                              (cooldown, effet de tir)
├─ EnemyWeapon.trackPlayerVelocity                       (vitesse du joueur pour anticiper)
├─ EnemyVision.canSee(this, target, sightLayers)
│  └─ angle ≤ 45° ∧ distance ≤ 180 ∧ Collision.hasLineOfSight
├─ EnemyAI.updateAlert(sees, target, delta)
├─ ¬alerté ? → EnemyWeapon.stopAttacking ; EnemyAI.patrol ; return
├─ canShoot = sees ∧ dist ≤ 150 ∧ Collision.hasLineOfSight(bulletLayers)
├─ ¬canShoot ∧ ¬(sees ∧ attaque en cours) ? → EnemyAI.chaseLastKnown ; return        → §4.4
└─ sinon : EnemyAI.stopWalking ; aim = vers lastKnown
   ├─ EnemyWeapon.updateAttack(delta, target, world)
   │  ├─ volleysLeft > 0 ? → fireVolley
   │  │  ├─ new Bullet(...) → World.bullets.add
   │  │  ├─ World.makeNoise(pos, 260, enemy)
   │  │  └─ recul, shootTimer
   │  ├─ winding ? → windupTimer ≤ 0 ? → démarre la salve
   │  └─ cooldown ≤ 0 ? → beginCharge  (direction anticipée : target + vitesse × temps de vol × 0,3)
   └─ EnemyWeapon.updateMotion                           (avance pendant la charge, recule au tir)
```

### 4.2 Chevalier — `SkullKnight.think`

```
SkullKnight.think
├─ attackTime ≥ 0 ? → updateAttack ; return              (le coup va toujours au bout)
│  └─ updateAttack
│     ├─ fente vers l'avant autour de l'impact (Enemy.moveForAI)
│     ├─ attackTime ≥ 0,2 s ∧ ¬hitDone ? → strike
│     │  ├─ distance ≤ 48 ∧ produit scalaire ≥ 0,17 (±80°) ∧ Collision.hasLineOfSight ?
│     │  └─ Player.hit(dir) → Character.takeDamage ; BloodEffects.burst si mort
│     └─ attackTime ≥ total ? → repos, cooldown 0,6 s
├─ joueur mort ? → return
├─ EnemyVision.canSee → EnemyAI.updateAlert
├─ ¬alerté ? → EnemyAI.patrol ; return
├─ sees ∧ dist ≤ 40 ? → stopWalking ; aim = vers cible ; cooldown ≤ 0 ? → attackTime = 0
└─ sinon EnemyAI.chaseLastKnown                          → §4.4
```

### 4.3 Réaction au parry

```
Enemy.parried(dir, from)
├─ mort ? return
├─ EnemyAI.alertAt(from)                  (même touché dans le dos, il sait où est le joueur)
├─ aim = from − position (normalisé)       (il se retourne)
├─ Stun.start(dir)                         (0,7 s ; recul 260 px/s freiné)
└─ onParried() [hook]
   ├─ ShooterEnemy : EnemyWeapon.stagger(1,2 s)   → stopAttacking + cooldown mini
   └─ SkullKnight  : attackTime = −1 ; cooldown = 1,1 s
```

### 4.4 IA — `EnemyAI`

```
EnemyAI.patrol(delta, walls)
├─ walking ? → vers wanderTarget (40 px/s) ; arrivé / bloqué → startPause
└─ sinon pauseTimer −= delta ; ≤ 0 → pickWanderTarget
   └─ 8 essais : point aléatoire à 20–80 px de home ; Pathfinder.isClear ? ok

EnemyAI.chaseLastKnown(delta, walls)
├─ arrivé (≤ 2 px) ? → path = null ; return
├─ repath (toutes les 0,4 s ou cible déplacée > 8 px) ? → Pathfinder.findPath(feetOf(pos), feetOf(lastKnown))
├─ point de passage suivant (retire ceux à < 3 px)
└─ Enemy.moveForAI(vers le point, runSpeed) → chasing = déplacement réussi ; sinon repath immédiat

EnemyAI.updateAlert(sees, target, delta)
├─ sees ? → alerted = true ; alertTimer = 1,5 ; lastKnown = target
└─ sinon ∧ alerté ∧ ¬chasing ? → alertTimer −= delta ; ≤ 0 → alerted = false, pause 0,5 s

World.makeNoise(at, radius, source)
└─ pour chaque Enemy ≠ source à ≤ radius : Enemy.hearNoise → EnemyAI.hearNoise → alertAt (si ¬seesPlayer)
```

---

## 5. Projectiles

```
BulletSystem.update(delta)
├─ pour chaque Bullet :
│  ├─ Bullet.update(delta, bulletLayers)
│  │  ├─ position += vitesse × delta ; mémorise la traînée (8 positions)
│  │  └─ durée > 3 s ∨ Collision.isSolid ? → alive = false
│  ├─ renvoyée ? → hitEnemies
│  │  └─ premier Enemy vivant touché : takeDamage ; BloodEffects.splatter ; Bullet.kill
│  └─ sinon hitPlayer
│     └─ touche le joueur vivant et non invincible :
│        ├─ Player.hit(dir) → Corpse.fall ; Character.takeDamage → Player.onDeath
│        ├─ BloodEffects.burst
│        ├─ onPlayerKilled.run → DeathSequence.start
│        └─ Bullet.kill
└─ bullets.removeIf(!alive)
```

---

## 6. Monde, collisions, pathfinding

```
Collision.isSolid(layers, px, py)          → case non vide d'un calque, ou hors carte
Collision.overlapsSolid(layers, rect)      → idem pour toutes les cases touchées par le rectangle
Collision.hasLineOfSight(layers, a, b)     → échantillonne un point tous les 4 px entre a et b

Character.move(dx, dy, solids)
├─ steps = ⌈max(|dx|, |dy|) / 4⌉
└─ pour chaque pas : x puis y séparément ; collidesWithSolid ? annule l'axe   (glisse le long des murs)

Pathfinder.findPath(layers, from, to)
├─ isClear(from, to) ? → liste vide
├─ nearestFree(to)                          (case libre à ≤ 3 cases)
├─ A* : file de priorité, 8 directions, interdit de couper un coin, ≤ 6000 expansions
├─ rebuild(parent, end)                     (remonte les parents)
└─ smooth(from, to, raw)                    (saute au point le plus lointain en ligne droite)
```

---

## 7. Rendu

```
GameRenderer.render(world, player, viewport, debug, slowCharge, slowIntensity)
├─ ScreenUtils.clear ; viewport.apply ; projection
├─ OrthogonalTiledMapRenderer.render
├─ BloodEffects.draw                          (sang au sol, sous tout)
├─ Enemy.drawVision *                         (cônes de vision)  → EnemyVision.draw
├─ Enemy.drawCharge *                         (point lumineux avant un tir)
├─ SpriteBatch :
│  ├─ Enemy.draw *    → EnemyAnimations.draw / SkullKnightAnimations.draw
│  └─ Player.draw     → PlayerAnimations.draw (+ DashEffects, Corpse)
├─ SpriteBatch : Bullet.draw *
├─ ShapeRenderer :
│  ├─ Enemy.drawAlert *
│  ├─ ShapeEffect.draw *                      (SlashEffects, ParryEffects)
│  ├─ BloodEffects.drawFlash ; ParryEffects.drawFlash
│  └─ SlowMoEffects.drawBar
├─ SlowMoEffects.drawOverlay(slowIntensity)
└─ debug ? → DebugRenderer.draw                (hitbox)
```

`ShadowRenderer` est appelé depuis les classes d'animation pour projeter l'ombre de chaque sprite.

---

## 8. Mort, pause, redémarrage

```
Mort du joueur
├─ cause balle      : BulletSystem.hitPlayer → onPlayerKilled → DeathSequence.start
├─ cause chevalier  : SkullKnight.strike → Player.hit ; GameScreen.update détecte vivant→mort → DeathSequence.start
└─ GameScreen.update : DeathSequence.advance → worldScale 0,25 ; GameScreen.render : isOver (0,45 s) → GameOverScreen

GameOverScreen.render
├─ R ou Entrée ? → restart (GameScreen.restart ; setScreen(level))  /  ESC ? → toMenu (level.dispose ; MainMenuScreen)
├─ level.updateEffects(delta)                  → Player.update (corps) ; BloodEffects.update
└─ level.drawFrame                             (image de la partie en dessous)

GameScreen.restart                              (touche R, « Recommencer », Game Over)
└─ enterLevel(currentEntry)                     (recommence l'étage courant, à son entrée)
   ├─ MapLoader.getEntry → Player.respawn
   │  └─ Ability.reset *  ; Corpse.reset
   ├─ spawnEnemies                              (vide les listes, recrée tous les gardes)
   ├─ GameCamera.centerOn
   ├─ DeathSequence.reset ; SlowMotion.reset ; HitStop.reset
   └─ BloodEffects.clear ; ShapeEffect.clear *

PauseScreen
├─ show  : Crosshair.restore (flèche normale)
├─ render: GameScreen.drawFrame                 (partie figée, sans update)
└─ « Reprendre » ou ESC → Main.switchTo(level) → GameScreen.show → Crosshair.apply
```

---

## 9. Réglages et menus

```
SettingsScreen
├─ Sliders volumes   → GameSettings.musicVolume / sfxVolume
├─ Plein écran       → GameSettings.fullscreen ; GameSettings.applyDisplay
├─ Remappage touche  → InputProcessor.keyDown → GameSettings.setKey(action, key)
├─ Réinitialiser     → GameSettings.resetKeys
└─ Retour            → GameSettings.save → onBack.run

GameSettings.pressed / justPressed(Action)      ← lu par PlayerInput.readMove et Player.update
```

---

## 10. Index des classes

| Package | Classe | Rôle en une ligne |
|---|---|---|
| (racine) | `Main` | `Game` libGDX : initialise, change d'écran |
| `camera` | `GameCamera` | viewport, suivi lissé, secousse |
| `entity` | `Entity` | position + 2 hitbox ; `update/draw` abstraites |
| | `Character` | vie (un coup = mort), visée, déplacement par pas |
| | `Direction` | haut/bas/gauche/droite à partir d'un vecteur |
| | `DirectionalAnimation` | 4 animations à partir d'une feuille de sprites |
| `entity.player` | `Player` | coordonne commandes, capacités, rendu |
| | `Ability`, `DashAbility`, `MeleeAttack`, `ParryAbility` | capacités |
| | `PlayerInput`, `Corpse`, `PlayerAnimations`, `DashEffects` | entrées, cadavre, rendu |
| `entity.enemy` | `Enemy` | base (template method) |
| | `ShooterEnemy`, `SkullKnight` | ennemis concrets |
| | `EnemyAI`, `EnemyVision`, `EnemyWeapon`, `Stun` | briques de comportement |
| | `EnemyAnimations`, `SkullKnightAnimations` | rendu |
| `entity.projectile` | `Bullet`, `LaserAssets` | laser et ses textures |
| `world` | `World`, `MapLoader`, `Door`, `ScreenFade`, `Collision`, `Pathfinder`, `BulletSystem` | état, carte, portes, fondu, collisions, A\*, balles |
| `render` | `GameRenderer`, `ShapeEffect`, `SlashEffects`, `ParryEffects`, `BloodEffects`, `SlowMoEffects`, `ShadowRenderer`, `MapShadows`, `DoorIndicator`, `DebugRenderer` | rendu |
| `screen` | `GameScreen`, `MainMenuScreen`, `PauseScreen`, `GameOverScreen`, `SettingsScreen` | écrans |
| | `HitStop`, `SlowMotion`, `DeathSequence` | échelles de temps |
| `ui` | `Theme`, `FilmOverlay`, `MenuBackground`, `Crosshair` | interface |
| `settings` | `GameSettings` | réglages persistants |
| `lwjgl3` | `Lwjgl3Launcher`, `StartupHelper` | lancement bureau |
