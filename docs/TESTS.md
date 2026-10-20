# Stratégie de tests

> **Statut honnête : le projet n'a aucun test automatisé pour l'instant.** Le sujet demande « un testing coverage pour
> garantir la conformité des nouveaux morceaux de code ». Ce document décrit ce qui est testable, comment, et dans quel ordre.
> Le dossier `core/src/test` n'existe pas encore.

## 1. Ce qui est testable sans fenêtre ni OpenGL

libGDX sépare les structures de données (testables) du rendu (qui demande un contexte GL). Les classes de logique pure
n'ont besoin que de `Vector2`, `Rectangle`, `MathUtils` et `TiledMapTileLayer` (simple grille de cellules, créable en code).

| Priorité | Classe | Ce qu'on teste | Difficulté |
|---|---|---|---|
| 1 | `Direction.of` | 4 directions, cas d'égalité, vecteur nul | triviale |
| 1 | `Collision` | `isSolid` (dedans / hors carte), `overlapsSolid`, `hasLineOfSight` avec un mur au milieu | facile (grille construite en test) |
| 1 | `Pathfinder` | ligne droite → liste vide ; mur entre les deux → chemin qui le contourne ; cible emmurée → `null` ; `MAX_EXPANSIONS` borne le coût | facile |
| 1 | `HitStop`, `SlowMotion`, `DeathSequence` | durées, échelles de temps, jauge qui se vide/se recharge, `reset()` | triviale |
| 2 | `Stun` | recul freiné, fin après `TIME`, `isActive` | facile (sous-classe de test d'`Enemy`) |
| 2 | `Character` | un coup = mort ; `isInvincible` bloque ; `respawn` ; `move` bloqué par un mur et glisse | facile (sous-classe de test) |
| 2 | `EnemyAI` | alerte après `canSee`, perte d'alerte après 1,5 s, `hearNoise` ignoré si le joueur est vu, `alertAt` | moyenne (dépend d'`Enemy`) |
| 2 | `Bullet` | déplacement, expiration à 3 s, mort au contact d'un mur, `reflect` (vitesse ×1,6) | facile (texture `null` tant qu'on ne dessine pas) |
| 2 | `BulletSystem` | balle ennemie tue le joueur, joueur invincible → pas de mort, balle renvoyée tue un ennemi | moyenne |
| 3 | `MapLoader` (parsing) | spawns lus depuis un `.tmx` de test | demande les assets |
| — | rendu, `Theme`, menus | **non testés automatiquement** | demande GL |

## 2. Blocages actuels

| Obstacle | Détail | Contournement |
|---|---|---|
| `Player` construit `PlayerAnimations` (charge des textures) | impossible sans GL | `Mockito`, ou extraire une interface `PlayerRenderer` |
| `Ability` dépend de `Player` | les capacités ne sont pas testables seules | tester via un `Player` mocké (`getAim`, `getPosition`) |
| `GameSettings` est statique et lit `Gdx.input` | difficile à simuler | `Gdx.input` mocké (`Gdx.input = mock(Input.class)`) |
| `World` crée directement ses effets visuels | pas de GL requis a priori, mais lourds | OK tel quel |
| `Enemy` : constantes de hitbox privées | rien de bloquant | — |

## 3. Mise en place proposée

À ajouter dans `core/build.gradle` :

```gradle
dependencies {
  testImplementation "org.junit.jupiter:junit-jupiter:5.10.2"
  testImplementation "org.mockito:mockito-core:5.11.0"
}
test { useJUnitPlatform() }
```

Couverture avec JaCoCo :

```gradle
plugins { id 'jacoco' }
jacocoTestReport { reports { html.required = true; xml.required = true } }
test { finalizedBy jacocoTestReport }
```

Rapport : `core/build/reports/jacoco/test/html/index.html`.

### Convention

- Un fichier de test par classe : `core/src/test/java/fr/epitech/katanagame/<package>/<Classe>Test.java`.
- Nom de méthode : `quandCondition_alorsRésultat` (ex. `ligneDroiteBloquee_contourneLeMur`).
- Aucun test ne dépend d'un `sleep` : les classes de temps prennent un `delta`, on les fait avancer à la main.
- Une grille de test se construit en une ligne :

```java
static TiledMapTileLayer grid(String... rows) {          // '#' = mur, '.' = libre
    TiledMapTileLayer layer = new TiledMapTileLayer(rows[0].length(), rows.length, 16, 16);
    for (int y = 0; y < rows.length; y++)
        for (int x = 0; x < rows[0].length(); x++)
            if (rows[rows.length - 1 - y].charAt(x) == '#') layer.setCell(x, y, new TiledMapTileLayer.Cell());
    return layer;
}
```

## 4. Exemples de cas à écrire en premier

```text
CollisionTest
  pointDansUnMur_estSolide
  pointHorsCarte_estSolide
  murEntreDeuxPoints_pasDeLigneDeVue
PathfinderTest
  cheminDirect_listeVide
  murVertical_cheminContourne
  cibleEntouréeDeMurs_null
HitStopTest
  parry_figeLeMondePuisLeRalentit
  attaque_figeMoinsLongtempsQueLeParry
SlowMotionTest
  maintenir_viderLaJauge_puisStopper
  jaugeTropBasse_neSeDeclenchePas
StunTest
  start_reculeEtRalentit
  apresTIME_nEstPlusActif
CharacterTest
  takeDamage_tueEnUnCoup
  invincible_neMeurtPas
  move_glisseLeLongDUnMur
BulletTest
  reflect_accelereEtMarqueRenvoyee
  expireApres3Secondes
```

## 5. Tests manuels (en attendant)

Liste de contrôle à rejouer avant chaque livraison :

- [ ] Le jeu se lance, le menu s'affiche, JOUER charge l'étage.
- [ ] Déplacements ZQSD, collisions avec murs/meubles, pas de traversée en dash.
- [ ] Dash invincible : un laser tiré pendant le dash ne tue pas.
- [ ] Clic gauche : un garde touché meurt, éclat blanc + arrêt sur image.
- [ ] Clic droit : un laser parré repart et tue un garde ; un chevalier parré est repoussé et sonné, même touché dans le dos.
- [ ] Les gardes accourent au bruit d'un kill/tir, contournent les murs (A\*).
- [ ] Mort : séquence ralentie → Game Over ; `R` / `Entrée` relance, `Échap` va au menu.
- [ ] Pause : image figée, Reprendre / Recommencer / Paramètres / Menu fonctionnent.
- [ ] Paramètres : remappage d'une touche → persiste après relance ; plein écran.
- [ ] Redimensionner la fenêtre : le rendu reste net, ratio préservé.
