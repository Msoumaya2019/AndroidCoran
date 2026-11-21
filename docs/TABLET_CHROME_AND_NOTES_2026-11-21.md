# Notes redimensionnables et lecture tablette compacte

Ces deux demandes utilisateur sont réalisées dans AndroidCoran uniquement. Aucune écriture dans Expo/Swift, aucune migration ou configuration Supabase, aucune modification des images des 604 pages, des polices coraniques ou du Tajweed.

## Notes

- Icônes de bloc-notes plus visibles : taille minimale de 24 dp, cible tactile de 44 dp, placement espacé dans la marge. La cible ne déborde pas sur le texte coranique.
- Les notes enregistrées s’ouvrent en consultation : texte et aperçu manuscrit, catégories informatives, bouton Modifier. Aucun champ de saisie ni outil de dessin avant Modifier. La taille initiale suit le contenu ; l’aperçu manuscrit est cadré autour des traits sans changer leurs coordonnées. Une longue note défile dans une fenêtre plus grande, limitée initialement à 82 % de la hauteur.
- Fenêtre déplaçable par sa poignée, redimensionnable par son coin inférieur droit, avec Agrandir/Réduire. Disponible en consultation, création et édition, au stylet ou au clavier. Les dimensions et le brouillon survivent à une recréation de l’écran. Une petite fenêtre d’édition conserve la zone blanche et le bouton Enregistrer ; les métadonnées restent accessibles dans un panneau séparé.
- Le canevas utilise une transformation uniforme. Les nouvelles notes manuscrites ajoutent noteAspect dans le JSON existant. Les anciennes notes conservent strictement leurs points, pressions, couleurs, identifiants et ancrages. En l’absence d’aspect enregistré, le format historique 1,4 est utilisé ; les proportions exactes d’une ancienne fenêtre différente ne peuvent pas être déduites de coordonnées normalisées seules.
- Enregistrer attend la sauvegarde locale durable, ferme la fenêtre puis revient à Lecture, sans recharger la page ni réinitialiser sa transformation. Les menus de note/forme et l’annotation active sont fermés. Un échec de stockage conserve le brouillon et la fenêtre. La synchronisation distante utilise toujours le mécanisme existant ; elle ne bloque pas la sauvegarde locale.

## Tablette : deux lignes

La refonte est activée uniquement sur appareil tablette (petit côté de la fenêtre maximale ≥ 600 dp) lorsque le mode tablette est actif. Un smartphone conserve ses commandes, même si un ancien réglage force le mode tablette. Le mode automatique continue à tenir compte de la fenêtre disponible.

1. Retour, titre Sourate · page/604 ouvrant le sélecteur existant, Plein écran et Options.
2. Lecture, Annoter, Mes notes, Révisions, avec icônes et onglet actif visible.

Lecture masque les outils ; les annotations restent dessinées. Annoter affiche tous les outils existants en bas, sans panneau latéral. Le passage Lecture/Annoter ne remesure pas la vue du Mushaf : le zoom et le déplacement restent identiques. Le mode Sélection dans Annoter permet de déplacer la page sans faire disparaître les outils.

Options regroupe Français/Arabe, double page, doigt, affichage, audio/programme et export. Les commandes audio et de validation existantes sont réutilisées dans un panneau ; elles n’ajoutent aucune ligne à l’en-tête. Les gestes et les petites flèches latérales assurent la navigation. Les pages restent centrées en portrait/paysage, la double page reste RTL. En portrait une préférence double page est conservée et signalée dans Options. Annoter depuis une double page revient à une seule page ; Annoter depuis la traduction revient au Mushaf arabe.

Plein écran masque les deux lignes ; son bouton de sortie les rétablit. Les outils restent utilisables si Annoter était actif. La barre flottante d’appui long, les notes, le cahier, les catégories et les exports sont conservés.

## Validation et limites

Implémenté ; Testé pour les parcours automatisés ci-dessous. Code applicatif 89f5015, sélecteurs de tests finalisés dans b254246. [Compilation Gradle, Lint, 200 tests JVM et APK réussis](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44931165088). [69 tests smartphone et 43 tests tablette Android 15 réussis](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44931165032), zéro échec et zéro test ignoré.

Tests des coordonnées et de la persistance, lecture seule, adaptation de fenêtre au contenu, erreur de stockage, cercle manuscrit non déformé après redimensionnement, reprise du brouillon, zoom/déplacement identiques après sauvegarde. Tests des deux lignes, modes Lecture/Annoter, notes, cahier, double page, plein écran, doigt et branche smartphone. En local : 18 tests notes/gestes et 12 tests tablette en paysage réussis. L’essai local en portrait a été interrompu par une instabilité de l’émulateur après changement de format (System UI ne répondait plus) ; il n’est pas déclaré réussi. Les captures locales masquées par cette alerte ne constituent pas une validation visuelle finale. Les parcours portrait/paysage automatisés sont couverts par la suite GitHub réussie.

APK de débogage livré : SHA-256 `9BF7DBDCF7FE644117DF9ADEB3457B68C1F382EC88992854FCDA47423DC4D042`. Certificat SHA-256 `c46c8bd18bd7007f33f33e61fbf0ae9048143771802b03fcaa216e38da50dfaa`, différent du précédent APK. La mise à jour directe peut donc être refusée par Android ; ne pas désinstaller une ancienne version contenant des données non sauvegardées. Référence Expo : 3 279 fichiers identiques au commit f538ae3, état Git propre ; aucune modification Swift.

Les tests d’interface utilisent un émulateur et des événements synthétiques. Stylet physique, rejet de paume et synchronisation d’un compte réel restent non validés. Un ancien canevas dont le rapport de dimensions n’a jamais été sauvegardé utilise le format historique décrit ci-dessus. La clé de signature debug GitHub peut différer entre APK : conserver l’ancienne installation tant que ses données ne sont pas sauvegardées.
