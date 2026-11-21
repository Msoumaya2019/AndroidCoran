# Lecteur et notes : neuf demandes du 21 novembre 2026

Travail exclusivement AndroidCoran. Aucun changement dans Expo, Swift, le backend, les polices coraniques, les images des 604 pages ou les données Tajweed.

| Demande | Implémentation native | Vérification |
|---|---|---|
| Zoom sous les doigts | ReaderTransform et ScaleGestureDetector conservent le point du papier sous le foyer ; translation du foyer pendant le pincement ; double appui centré | Tests JVM et événements MotionEvent réels sur émulateur |
| Surface d’écriture | Capture sur toute la vue, coordonnées non limitées aux marges initiales ; espace ajusté aux tracés existants et aux exports ; même transformation pour tracés, sélection et gomme | Sérialisation des coordonnées hors page, tests de stylet et exports |
| Fenêtre de note | Fenêtre flottante déplaçable par sa poignée ; titre éditable ; modes Stylet / Clavier ; grand espace blanc ; brouillon restaurable ; Enregistrer et confirmation d’abandon | Parcours de note, saisie et restauration |
| Catégories | Dix catégories initiales, plusieurs sélections, catégorie libre ; au moins une obligatoire pour enregistrer ; catalogue conservé dans reader.noteCategories ; filtre du cahier | Contrats JVM et parcours UI |
| Icônes de notes | Bloc-notes coloré dans la marge, couleur indépendante des tracés et catégories ; placement ordonné sans superposition ; appui ouvrant la note | Test des rectangles et ouverture ciblée |
| Gomme | Icône vectorielle de gomme à la place de la baguette, même action et emplacement | Compilation et régression gomme |
| Double page | Gouttière blanche de 24 dp, pages centrées avec proportions conservées ; annotations visibles ; édition en simple page | Régression RTL et capture visuelle |
| Page courbée | Maillage Canvas natif cylindrique ombré sur capture de l’ancienne page ; passage par boutons et gestes, sens inversé pour le retour ; préférence persistante dans les réglages | Tests des images intermédiaires et préférence |
| Appui long | Appui simple sans sélection ni menu, sauf icône de note ; appui long avec vibration système, sélection du verset et options existantes ; annulation pendant pincement, déplacement ou annotation | Événements MotionEvent, menus et régression |

## Conservation des données

Les champs title, categories et iconColor sont additifs dans readerInk et readerAnnotations. Les anciennes notes sans ces champs conservent leur texte, écriture, ancrage et tracés ; elles restent consultables sans catégorie. Une catégorie est demandée lors d’un nouvel enregistrement explicite. Le catalogue de catégories est conservé après suppression de sa dernière note. Aucune migration de table ni règle RLS n’est nécessaire.

Les tracés libres ordinaires continuent à être sauvegardés immédiatement. Les nouvelles notes restent en brouillon jusqu’à Enregistrer ; une fenêtre ouverte bloque les événements sur la page située derrière. La fermeture d’un brouillon modifié demande d’enregistrer ou d’abandonner. Les notes enregistrées peuvent être rouvertes depuis leur icône, la liste ou le cahier. Les notes textuelles/manuscrites ne sont toujours pas ajoutées en annexe des exports PNG/PDF.

## Limites et recette

L’effet de courbure est un rendu Canvas par maillage, pas une simulation physique 3D ; le choix de désactivation est disponible dans Réglages. Les animations système désactivées suppriment aussi les animations de durée Android. Le Tajwid simplifié utilise une liste de versets, sans feuille de Mushaf à courber. La double page conserve son mode de lecture : revenir à une seule page pour dessiner.

Les mesures de fluidité sur tablette physique, le rejet de paume matériel, les stylets réels et la synchronisation avec un compte Supabase réel doivent être vérifiés sur appareil. Aucune validation de ces points n’est revendiquée par les tests synthétiques.

## Résultats exécutés

Code final : e86ee1a9f082d6ef05f04e536a63724e1441b051. [Compilation, Lint et APK GitHub](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44919407755) réussis ; rapport HTML vérifié : 196 tests JVM, zéro échec et zéro ignoré. [Interface Android 15](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44919407698) : 60 tests téléphone et 33 tests tablette réussis, zéro échec, erreur ou test ignoré. Rapports XML des deux profils téléchargés et comptes vérifiés.

En local : sept tests ciblés réussis, puis 33 tests de lecteur au format 1600×1000 et les mêmes 33 au format 1000×1600, 160 dpi, sans échec. L’APK local utilisé pour ces parcours contient le lecteur 1f7fa55, identique au lecteur final e86ee1a ; ce dernier ajoute uniquement la correction du recyclage d’image de profil découverte par la régression générale. La courbure est également contrôlée par images intermédiaires déterministes dans les deux sens. Les tests de durée utilisent des animations système désactivées ; la fluidité matérielle réelle reste à mesurer.

Captures finales examinées : deux pages 2/1 complètes et centrées avec gouttière blanche ; fenêtre de note, grande zone blanche, catégories, palette, titre et bouton Enregistrer désactivé sans catégorie. Appui long manuel ouvrant les actions du mot et « Marquer comme difficile ». Première capture d’émulateur masquée par une alerte System UI : alerte fermée puis capture finale reprise ; aucune validation visuelle n’est fondée sur cette première capture. Émulateur isolé arrêté après restauration de sa taille et densité.

Conservation originale : 3 279 fichiers Expo vérifiés SHA-256, aucun changement, état Git propre. Aucun commit, branche ni push vers Expo ; Swift non modifié.

APK livré, SHA-256 : 65933A14380E49DB5295E4B946C617BEBEA9922EE0C88BA8BA7E645A46E7D1FB. Attention : la signature de débogage de cet artefact GitHub diffère de celle de l’APK précédent. Android peut refuser une mise à jour directe ; ne pas désinstaller une ancienne installation contenant des données non sauvegardées. La compatibilité du format de notes est testée, mais cela ne remplace pas la continuité de la clé de signature pour une mise à jour sur appareil. Aucun effacement de données utilisateur n’a été effectué. Une clé de signature de distribution durable devra être configurée pour les livraisons régulières.
