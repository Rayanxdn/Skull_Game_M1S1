# L'IA du game-service (Lot 2)

## Rôle de l'IA

L'IA joue à la place d'un joueur : soit parce qu'on l'a ajoutée à la création de la partie (`aiCount`), soit parce qu'un joueur humain a quitté une partie en cours. Elle est dans le package `fr.miage.skull.game.ai`.

## Ce qu'elle voit

`AiView` contient seulement ce qu'un joueur humain verrait : ses disques en main (`hand`), ses disques déjà posés (`ownStack`), l'enchère en cours (`currentBid`) et le nombre total de disques posés sur la table (`totalDiscsOnTable`). Elle ne reçoit jamais le contenu des piles des autres joueurs, donc elle ne peut pas tricher.

## Ce qu'elle décide

`AiAction` : poser un disque (`PlaceDisc`), enchérir (`Bid`) ou se retirer (`Withdraw`).

| Situation | Décision |
|---|---|
| Aucune enchère, rien posé encore | elle pose un disque pris au hasard dans sa main |
| Aucune enchère, elle ne peut plus poser | elle doit enchérir : son nombre de disques posés |
| Aucune enchère | 80 % elle pose un disque au hasard, 20 % elle enchérit |
| Enchère en cours, surenchère impossible (au-delà du total de disques) | elle se retire |
| Enchère en cours, seulement des fleurs sur sa pile | elle surenchérit de 1 avec 70 %, sinon elle se retire |
| Enchère en cours, un crâne sur sa pile | elle surenchérit de 1 avec 20 %, sinon elle se retire |

Pendant un défi, elle retourne d'abord sa propre pile, puis la pile d'un adversaire tirée au hasard. Le premier disque du placement initial est posé au hasard. Ces deux cas sont gérés dans `GameService`.

## Hasard et tests

Le hasard (`java.util.Random`) est fourni de l'extérieur à `decide(vue, random)`. Avec une graine fixe, les tirages sont toujours les mêmes, donc les tests sont reproductibles. Les 9 tests de `StandardAiStrategyTest` vérifient les fréquences (80 %, 20 %, 70 %, 20 %) sur 10 000 tirages avec une tolérance de 2 %, la surenchère de +1, l'enchère obligatoire sans disque en main et le repli sur le retrait.

## Intégration

`AiStrategy` est une interface (`id()`, `decide(...)`). L'implémentation du sujet est `StandardAiStrategy` (id `standard`). `GameService` convertit l'état du moteur en `AiView`, appelle l'IA et transforme sa réponse en coup (`Move`). Il enchaîne les tours des IA avec un plafond de 200 pas pour éviter toute boucle infinie.

## Remplacement d'un joueur

Si un joueur quitte une partie commencée, son siège est confié à une IA avec ses disques et ses points, et il est enregistré comme perdant. Si la partie n'a pas commencé, il est simplement retiré (règle à confirmer, la phrase du sujet est coupée).

## Limites connues

- Le joueur banni garde son jeton valide dans le game-service jusqu'à son expiration (voir `jwt-et-bannissement.md`).
- L'IA est volontairement simple (probabilités fixes du sujet), elle ne tient pas compte des coups des adversaires.
- `PlayerStyleStore` est réservé à une option future et ne fait rien pour l'instant.
