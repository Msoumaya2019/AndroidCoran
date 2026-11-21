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

Les mesures de fluidité sur tablette physique, le rejet de paume matériel, les stylets réels et la synchronisation avec un compte Supabase réel doivent être vérifiés sur appareil. Aucune validation de ces points n’est revendiquée par les tests synthétiques. Résultats de compilation et de tests à compléter après exécution finale.
