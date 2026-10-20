# Architecture technique

## 1. Pile technique

| Élément | Choix |
|---|---|
| Langage | Java (sources en `sourceCompatibility = 8`, voir §8) |
| Moteur | [libGDX](https://libgdx.com/) 1.14.2 (`gdx`, `gdx-freetype`) |
| Backend bureau | LWJGL3 3.4.3 |
| Cartes | [Tiled](https://www.mapeditor.org/) (`.tmx`), chargées par `TmxMapLoader` |
| Build | Gradle (wrapper fourni) — modules `core` et `lwjgl3` |
| Éditeur de sprites | Aseprite (sources `.aseprite` dans `assets/`) |

## 2. Modules Gradle

```
2dgame/
├── core/        logique du jeu, indépendante de la plateforme (tout le code de gameplay)
│   └── src/main/java/fr/epitech/katanagame/...
├── lwjgl3/      lanceur bureau : Lwjgl3Launcher, StartupHelper, configuration de la fenêtre
├── assets/      ressources lues à l'exécution (cartes, sprites, polices, lasers)
├── docs/        cette documentation
└── build.gradle, settings.gradle, gradle.properties
```

`lwjgl3` dépend de `core` ; `core` ne dépend d'aucune plateforme → un autre backend (HTML, mobile) pourrait réutiliser `core` tel quel.

## 3. Organisation du code (`core`)

Voir le diagramme des packages dans [`UML.md §1`](UML.md#1-vue-densemble-des-packages).

| Package | Responsabilité | Ne doit pas |
|---|---|---|
| `screen` | cycle de vie des écrans, **coordination** de la partie, échelles de temps | contenir de la logique d'entité |
| `entity` / `entity.player` / `entity.enemy` / `entity.projectile` | acteurs du jeu et leurs comportements | dessiner autre chose que leur propre sprite |
| `world` | état partagé (`World`), carte, collisions, A\*, balles | connaître les écrans |
| `render` | tout le dessin de la partie (`SpriteBatch`, `ShapeRenderer`) | modifier l'état du jeu |
| `ui` | thème, menus, viseur | connaître la logique de jeu |
| `settings` | préférences persistantes | — |
| `camera` | viewport, suivi, secousse | — |

## 4. Cycle de vie et boucle de jeu

libGDX appelle `ApplicationListener` : `create → (resize, render)* → dispose`. `Main` hérite de `Game` et délègue à un `Screen`.

```
create()  ──►  MainMenuScreen ──JOUER──► GameScreen ◄──── PauseScreen (Échap / Reprendre)
                     ▲  ▲                    │  ▲
                     │  └──── Menu ──────────┘  └──── GameOverScreen (mort) ──► restart / menu
                     └──── SettingsScreen (retour vers l'écran d'origine)
```

- **Un seul thread** : toute la logique et le rendu vivent dans `render(delta)`. Pas de synchronisation à gérer.
- **Pas de pas de temps fixe** : les `update` reçoivent le `delta` réel de la frame (multiplié par les échelles de temps).
  Le déplacement est donc découpé en pas ≤ 4 px (`Character.move`) pour ne pas traverser les murs à basse fréquence d'images.
- **Changement d'écran** : `Main.switchTo` change puis `dispose()` l'ancien. Depuis un clic sur un bouton, l'action est reportée
  à la frame suivante (`Gdx.app.postRunnable`) pour ne pas détruire un `Stage` pendant qu'il traite l'événement.
- **Pause / Game Over** : ces écrans ne détruisent pas la partie ; ils appellent `GameScreen.drawFrame()` (image figée)
  et, pour le Game Over, `GameScreen.updateEffects()` (le corps et le sang continuent).

## 5. Ressources et durée de vie

| Ressource | Créée | Libérée |
|---|---|---|
| `Theme`, `FilmOverlay`, `MenuBackground` | `Main.create` | `Main.dispose` (partagées par tous les menus) |
| `Stage` d'un menu | constructeur de l'écran | `Screen.dispose` |
| Carte, `GameRenderer`, `Player`, textures ennemis/lasers, ombres | constructeur de `GameScreen` | `GameScreen.dispose` (appelé en quittant vers le menu) |
| Textures laser (`LaserAssets`) | `Enemy.loadAssets` | `Enemy.disposeAssets` |

Règle de projet : toute classe qui possède une ressource native implémente `Disposable`.
`GameScreen` charge les assets ennemis une fois à sa création. À chaque changement d'étage (`loadLevel`), la carte, ses ombres (`MapShadows`) et le `GameRenderer` de l'ancien étage sont libérés avant d'installer ceux du nouveau ; le `Player` et la caméra restent les mêmes.

## 6. Données de niveau (Tiled)

`assets/maps/etage_1.tmx` (70 × 50 tuiles de 16 px), `etage_2.tmx`, `boss.tmx`. Chaque carte suit le même contrat, vérifié par `MapLoader`.

| Calque | Type | Rôle |
|---|---|---|
| `Calque de Tuiles 1` | tuiles | sol |
| `Mur` | tuiles | bloque corps, balles **et** vue |
| `Objets` | tuiles | bloque corps et balles (pas la vue) |
| `Chaises` | tuiles | bloque les corps seulement |
| `Petits Objets` | tuiles | décor, sans collision |
| `Spawns` | objets | `player_spawn` (entrée par défaut), `entry_xxx` (arrivée par une porte), `skull_knight`, tout autre objet = tireur |
| `Doors` | objets | rectangles : `target_map`, `target_entry`, `requires_clear` (vrai par défaut) |

Une case non vide d'un calque de collision est solide ; sortir de la carte est considéré comme solide.
`MapLoader.requireLayer` lève une `IllegalStateException` si un calque obligatoire manque (échec rapide plutôt que bug silencieux).

## 7. Construire, lancer, documenter

```bash
./gradlew lwjgl3:run          # lancer le jeu
./gradlew lwjgl3:jar          # jar exécutable → lwjgl3/build/libs
./gradlew core:javadoc        # Javadoc → core/build/docs/javadoc/index.html
./gradlew test                # tests unitaires (voir TESTS.md)
./gradlew clean build
```

Raccourcis en jeu utiles au développement : `F1` (hitbox), `F2` (mode dieu) ; `R` après la mort.

## 8. Dette technique connue

| Point | Détail |
|---|---|
| Version de Java | `build.gradle` déclare `sourceCompatibility = 8`, mais `World` utilise `List.of` (Java 9). À corriger : passer à 17 ou remplacer. |
| Audio | volumes réglables mais aucun son joué. |
| Premier étage en dur | `GameScreen.FIRST_MAP = "maps/etage_1.tmx"` ; la suite est pilotée par les portes de chaque carte. |
| `ScreenFade` | est dans le dossier `world/` mais déclare `package …render` : à harmoniser. |
| Tests | aucun pour l'instant. |
| Assets | le dossier `assets/` contient beaucoup de packs non utilisés (taille du dépôt). |
