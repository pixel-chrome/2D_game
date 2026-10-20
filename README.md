# LAME D'AUBE — Katana Omega

Jeu d'action **top-down « un coup = mort »** en Java ([libGDX](https://libgdx.com/)), inspiré de *Katana Zero* :
on esquive (dash), on dévie les tirs (parry), on tranche, et on ralentit le temps. Projet EPITECH B-OOP-301 (2D Game).

> Une lame. Un dash. Un seul coup.

## Lancer le jeu

```bash
./gradlew lwjgl3:run
```

| Action | Entrée |
|---|---|
| Déplacement | `Z` `Q` `S` `D` |
| Coup de sabre | clic gauche |
| Parry (renvoie les lasers, assomme au contact) | clic droit |
| Esquive (invincible) | `Espace` |
| Ralenti | `Maj` gauche (maintenu) |
| Pause / menu | `Échap` |
| Recommencer | `R` ou `Entrée` (écran Game Over) |
| Hitbox de debug | `F1` |
| Mode dieu (test) | `F2` |

## Documentation

| Document | Contenu |
|---|---|
| [`docs/GDD.md`](docs/GDD.md) | Game Design Document : concept, contrôles, joueur, ennemis, niveau |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | modules, cycle de vie, ressources, données de niveau, dette technique |
| [`docs/UML.md`](docs/UML.md) | diagrammes de classes, d'états et de séquence (Mermaid) |
| [`docs/PATTERNS.md`](docs/PATTERNS.md) | principes OOP / SOLID et design patterns, avec renvois au code |
| [`docs/FONCTIONS.md`](docs/FONCTIONS.md) | arbres d'appels de fonctions |
| [`docs/BOSS.md`](docs/BOSS.md) | préparation du boss final (conception, pas encore codé) |
| [`docs/TESTS.md`](docs/TESTS.md) | stratégie de tests et liste de contrôle manuelle |
| Javadoc | `./gradlew core:javadoc` → `core/build/docs/javadoc/index.html` |

## État

Trois cartes enchaînées par des portes (étage 1, étage 2, arène du boss vide), deux types d'ennemis, trois capacités + ralenti, IA avec A\*. Pas encore : audio, boss final, enchaînement de niveaux, tests automatisés
(voir le GDD §10).

---

*Le reste de ce fichier vient du gabarit gdx-liftoff.*

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
