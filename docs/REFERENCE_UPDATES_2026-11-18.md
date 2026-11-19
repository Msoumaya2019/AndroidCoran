# Vérification de la référence — 18 novembre 2026

> Historique : l’adaptation tablette et le format d’annotation décrits ici sont remplacés par la [mise à jour du 19 novembre](REFERENCE_UPDATES_2026-11-19.md). Les résultats du 18 novembre concernent uniquement le code daté de cette journée.

Lecture seule du dépôt Msoumaya2019/coran-memoire. main reste f538ae37565abf70032e7e215fe57c8b96c2152f : aucune nouveauté sur la branche principale.

La branche feat/roqya-audio-space est passée de 79631ce3d55d51a64a86366a74127b1ef4a7d890 à b0afa0f3f177c99b401d3ec427187b8ba0eb7c81. Elle reste distincte de main. Les cinq nouveaux commits sont :

- e2003e4 : durée de dernière session au premier plan et rapport administrateur.
- 9af81fa : destinataires dans la modération.
- cd64f82 : correction de la file de sessions et erreurs administrateur explicites.
- ec3264f : quiz solo aléatoires et réponses administrateur.
- b0afa0f : présentation du quiz solo.

Portage Android : ForegroundUsage/UsageStore, AdminService/AdminScreen, AdminModerationPanel, SoloQuiz/SoloQuizPanel et AdminQuizResponsesPanel. Source facultative des questions admin conservée selon la nouvelle règle. API identiques, aucun changement de schéma ou migration exécuté. Les fonctions serveur peuvent nécessiter les migrations de référence déjà prévues ; leur présence et les droits ne sont pas validés avec un compte réel. Toute application de migration reste soumise à autorisation explicite.

Les quatre commits plus anciens de cette branche (notamment l’ensemble Roqya) ne sont pas fusionnés automatiquement dans AndroidCoran. Leur portage reste à inventorier séparément ; ce document ne prétend pas à une parité complète de la branche.

## Tablettes et annotation demandée

Navigation latérale dès 600 dp de largeur avec hauteur suffisante, formulaires/listes centrés et limités à 1040 dp, accueil à deux colonnes et grille adaptative de sourates, lecture plein écran sans barre latérale. Le dimensionnement suit la fenêtre disponible, y compris le partage d’écran.

Mushaf QCF : contours libres et notes manuscrites, pression du stylet, aucune reconnaissance ni conversion en texte. Coordonnées relatives à la page ; notes dans un canevas au ratio constant. Un tracé ouvre une note ; toucher un contour ou la liste de page permet de la rouvrir. Suppression confirmée du contour et de la note, tombstones empêchant la réapparition après fusion concurrente.

Les données sont dans readerInk du JSON utilisateur existant et sauvegardées localement par le mécanisme habituel. Aucune table ajoutée. Une copie locale indépendante par compte conserve les annotations quand un ancien état distant omet le champ. Compatibilité réelle aller-retour Expo/Android et restauration sur un nouvel appareil à valider : une ancienne application peut ne pas conserver des champs JSON qu’elle ne connaît pas. Ne pas annoncer une synchronisation inter-applications validée.

Tests : cas JVM de portage, de fusion et de suppression ; tests Android de stylet, restauration et confirmation ; suite tablette distincte prévue. Validation du commit 59f7b77 : 176 tests JVM et lint réussis ; GitHub Actions Android 15 : 43 tests téléphone et 13 sur profil tablette Pixel C, sans échec ni test ignoré. En local : 43 tests téléphone et 15 tests dans chacun des deux formats tablette portrait/paysage. Parcours complet du contour à la suppression validé sur émulateur. Contrôles visuels de l’accueil, de la grille et du Mushaf effectués sur émulateur.
