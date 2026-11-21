# Barre flottante des versets et icône fournie

La maquette jointe sert de référence visuelle. Ses textes ne constituent pas des instructions supplémentaires. Travail exclusivement dans AndroidCoran ; Expo, Swift, les ressources coraniques et le backend ne sont pas modifiés.

## Comportement

L’appui long conserve la sélection du verset et la vibration système. La barre est dessinée au-dessus du lecteur, sans participer à sa mesure : aucun panneau inférieur ni commande générale ne s’ouvre automatiquement. L’appui simple sur le texte ne fait rien ; les icônes de notes gardent leur ouverture directe.

La barre affiche Écouter, Difficile, Note, Enregistrer ma récitation et X. Le texte complet de l’enregistrement est visible, avec retour à la ligne sur smartphone. Les actions prennent le numéro global du verset sélectionné. Difficile conserve la barre ouverte et indique son état ; un second appui retire le marqueur personnel. Les règles de difficulté existantes sont réutilisées, notamment pour les marqueurs administrateur.

Note ouvre le brouillon d’annotation du verset dans l’éditeur existant (titre, manuscrit/clavier, catégories et enregistrement). En double page, cette action revient à une seule page pour éditer. L’action d’enregistrement ouvre directement le panneau de récitation existant, limité au seul verset choisi ; ses règles de compte, d’information et d’autorisation micro restent applicables. Commencer déclenche ensuite la capture comme auparavant. La commande générale d’enregistrement conserve sa portée de page/programme.

Le bouton supplémentaire ⋯ conserve les fonctions de mot, répétition, formes et marque-page. Dans le lecteur imprimé, X et Retour ferment la sélection. Une navigation ou un déplacement/zoom ferme la barre afin de ne pas laisser une position devenue obsolète. La barre tient compte de l’origine réelle de chaque vue de page et de son zoom ; elle se place au-dessus de la ligne touchée, ou en dessous si l’espace manque, et reste bornée à la fenêtre. La ligne choisie reste dégagée ; comme toute barre superposée, elle peut recouvrir une partie d’une ligne voisine lorsque la page occupe toute la hauteur.

## Icône

L’image « Icône premium du Coran et des notes.png » fournie est convertie en PNG de lanceur mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi, avec une variante adaptative Android. Aucun dessin de remplacement n’est généré. Le manifeste référence ic_launcher pour l’icône normale et ronde. Le masque final dépend du lanceur Android ; une marge de sécurité préserve les éléments centraux.

## Vérifications

Compilation et tests finaux réussis. VerseFloatingBarTest vérifie les cinq commandes visibles, l’absence de réduction de la page, le texte intégral du bouton de récitation sur petit écran, l’ajout/retrait de difficulté, le brouillon de note et le passage exact transmis à l’enregistreur. Les suites de gestes, notes, audio et plein écran existantes sont conservées. Aucun compte réel, enregistrement micro réel ou synchronisation Supabase réelle n’est revendiqué par ces tests.

## Résultats exécutés

Code vérifié : 560c3fe807ed87fcce29729626ba69b3ac35b7a4. [Compilation, Lint et APK](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44922254930) réussis : 196 tests JVM, zéro échec et zéro ignoré. [Interface Android 15](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44922254926) : 63 tests téléphone et 36 tests tablette réussis, zéro échec, erreur ou ignoré. Rapports HTML/XML téléchargés et comptes vérifiés.

En local : les trois nouveaux tests ont réussi sur smartphone (1080×2400, 420 dpi) et tablette paysage (1600×1000, 160 dpi). Les captures des deux formats ont été examinées : texte complet de l’enregistrement visible et bande placée au-dessus de la ligne sélectionnée. Les assertions comparent exactement les dimensions et la position de la page avant/après ouverture. Ajout/retrait de difficulté, ouverture du brouillon sans sauvegarde implicite et plage d’un seul verset dans le panneau d’enregistrement vérifiés. Icône PNG de lanceur examinée ; ressources adaptatives et manifeste compilés. Le masque final propre à chaque lanceur physique reste à vérifier. Aucun enregistrement micro ni compte réel utilisé.

Référence Expo : 3 279 fichiers vérifiés SHA-256 inchangés et état Git propre. Swift non modifié. Émulateur de test isolé arrêté après restauration de sa taille et densité.

APK livré, SHA-256 : 972FA9A3780C8CE0BDC194E69542902123E55267D77A2A294CE4C58F7E47DF00. Signature debug GitHub différente de l’artefact précédent : une mise à jour directe peut être refusée. Conserver l’ancienne installation tant que ses données ne sont pas sauvegardées ; aucune désinstallation utilisateur n’a été effectuée.
