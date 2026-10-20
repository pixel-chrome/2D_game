# Game Design Document — LAME D'AUBE (Katana Omega)

> Nom de travail : **LAME D'AUBE** (titre affiché, `Main.TITLE`) · nom du projet Gradle : **Katana Omega**.
> Genre : action top-down « un coup = mort », inspiré de *Katana Zero* et *Hotline Miami*.
> Technologie : Java + libGDX (LWJGL3), cartes Tiled.

## 1. Concept

Le joueur est un samouraï vu **du dessus**. Il traverse un étage rempli de gardes. **Un seul coup tue**,
pour lui comme pour eux. Le jeu récompense la lecture du terrain, le timing et l'agressivité :
on esquive (dash), on dévie les tirs (parry), on tranche, et on ralentit le temps pour planifier.

> Accroche : « UNE LAME. UN DASH. UN SEUL COUP. »

Piliers de design :

| Pilier | Ce que ça implique dans le code |
|---|---|
| **Mort instantanée, relance immédiate** | `Character.takeDamage()` tue en un coup ; `R` relance sans écran de chargement (`GameScreen.restart()`) |
| **Lisibilité** | cônes de vision, point de charge avant un tir, indicateur « ! » d'alerte, teinte rouge avant un coup d'épée |
| **Le skill avant les stats** | pas d'XP ni d'inventaire : seules la lecture des patterns et l'exécution comptent |
| **Sensation (game feel)** | arrêt sur image (`HitStop`), secousse de caméra, ralenti (`SlowMotion`), sang, traînées de dash |

## 2. Perspective et vue

- **Vue** : 2D top-down (légère profondeur par les ombres projetées, `ShadowRenderer`).
- **Résolution logique** : 480 × 270 unités (1 unité = 1 pixel de la carte), mise à l'échelle par un `FitViewport`.
- **Caméra** : suit le joueur avec lissage, décalée de 15 % vers la souris, bornée à la carte, avec secousse.
- **Tuiles** : 16 × 16 px, carte `assets/maps/etage_1.tmx` (Tiled).

## 3. Boucle de jeu

```
Menu principal ─► Partie ─┬─ Échap ─► Pause ─► (Reprendre | Recommencer | Paramètres | Menu)
                          └─ Mort du joueur ─► courte séquence ralentie ─► Game Over ─► (R / Entrée : recommencer | Échap : menu)
```

Dans une partie : explorer → repérer les gardes → les éliminer (ou les éviter) → survivre.
Recommencer est volontairement instantané : l'échec doit coûter quelques secondes, pas plus.

## 4. Contrôles

| Action | Entrée par défaut | Remappable |
|---|---|---|
| Se déplacer | `Z` `Q` `S` `D` | oui (Paramètres) |
| Viser | souris | non |
| Coup de sabre | clic gauche | non |
| Parry | clic droit | non |
| Esquive (dash) | `Espace` | oui |
| Ralenti | `Maj` gauche (maintenu) | oui |
| Pause | `Échap` | non |
| Recommencer (écran Game Over) | `R` ou `Entrée` (`Échap` = menu) | non |
| Afficher / cacher les hitbox | `F1` | non |

Les touches remappables sont sauvegardées entre deux lancements (`GameSettings`, préférences libGDX).
Le mode debug (hitbox) est **activé par défaut** pendant le développement (`GameScreen.debug = true`).

## 5. Le joueur

Un coup = mort. Aucun point de vie. Trois capacités, partageant un cycle « durée d'action + recharge »
(classe abstraite `Ability`) :

| Capacité | Entrée | Effet | Durée / recharge |
|---|---|---|---|
| **Déplacement** | ZQSD | vitesse de 200 px/s, collisions avec les murs par les « pieds » | — |
| **Dash** | Espace | élan de 460 px/s dans la direction de déplacement (ou de visée) ; **invincible** pendant le dash | 0,12 s / 0,75 s |
| **Coup de sabre** | clic gauche | fente de 340 px/s vers la souris pendant 0,14 s ; **tue tout ennemi dans la hitbox** du coup, sur toute la fente | animation 0,56 s / 0,5 s |
| **Parry** | clic droit | fenêtre de 0,22 s devant le joueur : **renvoie les balles** (×1,6 de vitesse, elles tuent alors les ennemis) et **assomme les ennemis au contact** | 0,22 s actif / 0,45 s, **recharge annulée si le parry réussit** |
| **Ralenti** | Maj (maintenu) | monde ×0,2, joueur ×0,6 ; jauge de 1,2 s qui se recharge en 4 s | — |

Règles d'interaction :

- Un parry lancé **annule** l'animation du coup de sabre en cours (`melee.cancel()`).
- On ne peut pas parer **pendant** un dash.
- Un coup qui touche un ennemi tué déclenche un **bruit** (rayon 110) qui alerte les gardes proches.
- Un parry réussi déclenche un arrêt sur image (0,09 s) puis un ralenti du monde (0,35 s à ×0,3) : le joueur garde la main.

## 6. Les ennemis

Tous héritent d'`Enemy` : même IA de base (patrouille → alerte → poursuite), même vision, même réaction au parry.
Ils meurent en un coup, comme le joueur.

### 6.1 Comportement commun (`EnemyAI`, `EnemyVision`)

| Paramètre | Valeur |
|---|---|
| Champ de vision | cône de 90°, portée 180 px, bloqué par les murs (calque `Mur` uniquement) |
| Perte de vue | l'alerte retombe 1,5 s après être arrivé sur le dernier endroit connu |
| Patrouille | marche à 40 px/s vers un point aléatoire à 20–80 px de son point d'apparition, pause de 0,8–2 s |
| Poursuite | course vers le dernier endroit où le joueur a été vu / entendu, **A\*** pour contourner les murs |
| Bruit | un tir (rayon 260), un kill (110) ou un parry (120) attire les autres gardes ; ils ne sont pas alertés par leur propre bruit |

États (voir `UML.md` pour le diagramme d'états) : `PATROUILLE → ALERTE/POURSUITE → ATTAQUE → (perte de vue) → PATROUILLE`,
plus l'état transversal `SONNÉ` (parry) et `MORT`.

### 6.2 Types d'ennemis

| | **Tireur** (`ShooterEnemy`) | **Skull Knight** (`SkullKnight`) |
|---|---|---|
| Rôle | distance | corps à corps, rapide |
| Vitesse de course | 70 px/s | 130 px/s |
| Attaque | canalise 0,2 s (point lumineux annonce la direction), tire un laser (500 px/s), recul, cadence 1,2 s | à ≤ 40 px : préparation 0,2 s (teinte rouge), coup à portée 48 px sur ±80°, fente, recharge 0,6 s |
| Anticipation | **vise devant le joueur** (prédiction de vitesse, facteur 0,3) | non |
| Contre | parry (renvoie le laser), dash pendant la canalisation | dash **avant** l'impact (0,2 s), parry (assomme et annule le coup) |
| Condition de tir/coup | voit le joueur, ≤ 150 px et aucun mur entre eux | voit le joueur, ≤ 40 px, aucun mur entre eux |

### 6.3 Réaction au parry (`Stun`)

Un ennemi parré est **repoussé** (260 px/s freiné), **sonné 0,7 s**, **se retourne vers le joueur** même touché dans le dos,
et sa séquence d'attaque est annulée (le tireur ne peut plus tirer avant 1,2 s, le chevalier avant 1,1 s).

## 7. Monde et niveau

- Carte Tiled `etage_1.tmx`, calques : `Calque de Tuiles 1` (sol), `Mur`, `Objets`, `Chaises`, `Petits Objets` (décor), objets `Spawns`.
- Trois jeux de collisions :
  - **mouvement** : `Mur` + `Objets` + `Chaises` ;
  - **balles** : `Mur` + `Objets` (les chaises n'arrêtent pas un tir) ;
  - **vue** : `Mur` seulement (on voit au-dessus des meubles).
- Points d'apparition lus dans le calque d'objets `Spawns` :
  - `player_spawn` → joueur ;
  - `skull_knight` → chevalier ;
  - tout autre objet → tireur.
- Étage actuel : carte de 70 × 50 tuiles (1120 × 800 px), 1 joueur, 4 chevaliers et 19 tireurs. `etage_2.tmx` existe mais n'est pas encore branché.

## 8. Interface

| Écran | Contenu |
|---|---|
| **Menu principal** | titre, katana animé en fond, JOUER / PARAMÈTRES / QUITTER |
| **Partie** | viseur personnalisé, jauge de ralenti (en bas), indicateurs d'alerte au-dessus des gardes |
| **Pause** (Échap) | reprendre, recommencer, paramètres, quitter ; l'image de la partie reste affichée derrière |
| **Game Over** | s'affiche 0,45 s après la mort ; le sang et le corps continuent de s'animer en dessous |
| **Paramètres** | volumes (musique, effets), plein écran, remappage des touches |

Direction artistique : pixel art, voile et vignette « film » (`FilmOverlay`), thème sobre et saturé (`Theme`).

## 9. Game feel

| Effet | Déclencheur | Classe |
|---|---|---|
| Arrêt sur image 0,09 s + ralenti | parry réussi | `HitStop` |
| Arrêt sur image 0,06 s | coup de sabre qui touche | `HitStop` |
| Ralenti volontaire | touche RALENTI | `SlowMotion` |
| Mort du joueur : 0,06 s figé puis monde à ×0,25 | joueur touché | `DeathSequence` |
| Secousse de caméra (2,5 / 3 / 4) | dash / coup / parry | `GameCamera.shake` |
| Sang, taches, mare | mort d'un personnage | `BloodEffects` |
| Traînée et images rémanentes | dash | `DashEffects` |
| Croissant, étincelles | coup de sabre / parry | `SlashEffects`, `ParryEffects` |
| Ombres projetées | toujours | `ShadowRenderer` |

## 10. Périmètre actuel (POC) et ce qui manque

**Fait** : boucle de jeu complète, 1 niveau, 2 types d'ennemis, 3 capacités + ralenti, IA avec pathfinding A\*,
menus, paramètres persistants, effets de rendu.

**Pas encore fait** (honnêtement) :

- **Audio** : les réglages de volume existent (`GameSettings.musicVolume/sfxVolume`) mais aucun son n'est joué.
- **Boss final** dans la dernière pièce : préparé dans la conception, pas implémenté → voir [`BOSS.md`](BOSS.md).
- **Progression de niveaux** : `MapLoader` charge toujours `maps/etage_1.tmx` ; pas d'écran de victoire.
- **Tests automatisés** : aucun pour l'instant → voir [`TESTS.md`](TESTS.md).

## 11. Évolutions prévues

1. Boss final (dernière pièce) — [`BOSS.md`](BOSS.md).
2. Enchaînement des étages (`etage_2.tmx`) et écran de victoire.
3. Audio (musique + effets) branché sur les réglages existants.
4. Suite de tests unitaires sur la logique pure (collision, A\*, minuteurs, IA).
