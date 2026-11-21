# Actualisation Expo 0.9.40 / 0.9.41 — 21 novembre 2026

## Audit en lecture seule

Main Expo reste à 42dedf8079b26bf28ee87a530e82274bb5e1e87e. La branche feat/tablet-notebook passe de 22b3090 à [bb6b29588afa8d26c13127239a47bc9f1408e363](https://github.com/Msoumaya2019/coran-memoire/commit/bb6b29588afa8d26c13127239a47bc9f1408e363), après [147d5a5](https://github.com/Msoumaya2019/coran-memoire/commit/147d5a566091e03fb5e27476af6f67656d0a8795). Les branches Roqya et petites sourates n’ont pas changé. Comparaison GitHub et copies de consultation indépendantes dans work/reference-updates-2026-11-21 ; aucune écriture dans le clone de référence ni sur GitHub Expo.

| Origine | Fonction / données | Équivalent natif | Développement | Test |
|---|---|---|---|---|
| core/annotationWorkspace.ts, coranTest/inkLayer.ts, ui/NativeInkSurface.tsx | Marges blanches externes 35 % à gauche/droite et 8 % en haut/bas ; coordonnées relatives à la page, négatives admises | AnnotationWorkspace.kt, MushafView.kt, ReaderInk.kt | Implémenté | Testé |
| ui/ReaderPrintedPage.tsx, services d’export | Même repère pour rendu, sélection, gomme, persistance et PNG/PDF | MushafView.kt, AnnotationExport.kt | Implémenté | Testé |
| ui/ReaderAnnotationEditor.tsx, ui/NoteHandwriting.tsx | Autorisation explicite du doigt ou d’un stylet non reconnu dans le commentaire ; capture du geste | ReaderInkPanel.kt, HandwritingView.kt | Implémenté | Testé |
| App.tsx, core/tablet.ts | Bouton Doigt persistant, activation de l’annotation et des outils | TabletReader.kt, reader.tablet.stylusOnly | Implémenté | Testé |
| core/tablet.ts, App.tsx | Annotation en plein écran simple page ; outils flottants, accès au doigt | TabletReaderControls, TabletReader.kt | Implémenté | Testé |
| App.tsx, core/tablet.ts | Double page agrandie et jointe, plein écran en paysage, lecture seule ; notes masquées mais conservées ; préférence portrait avec indication de rotation | Tablet.kt, TabletReader.kt | Implémenté | Testé |
| MushafPage.tsx, coranTest/html.ts | Audio jaune prioritaire sur difficulté/marque-page, glyphes inchangés | MushafView.kt, TajwidReader.kt | Implémenté | Testé |
| App.tsx | Actions rapides réutilisant la note existante d’un mot/verset et conservant commentaire/écriture ; formes rapides sans ouverture forcée de l’éditeur | TabletReader.kt | Implémenté | Non commencé |

## Surlignage continu demandé le 21 novembre

Demande utilisateur ajoutée pendant le portage : la récitation doit surligner tout le verset sans espaces non colorés entre ses mots. Le rendu QCF rassemble les rectangles des mots du verset par ligne et dessine une seule bande derrière les glyphes. Les éditions en images regroupent les rectangles par numéro de ligne ; les espaces entre deux lignes restent libres. La lecture simplifiée utilise déjà un fond continu pour la carte du verset. Le lecteur partagé applique ce comportement à la lecture libre, l’apprentissage et la révision.

Un contrôle visuel a aussi révélé que le fond d’un Canvas de page pouvait effacer la page voisine. Chaque rendu est maintenant découpé explicitement à ses propres limites. Un test de pixels sur deux pages peintes sur la même surface couvre ce cas, au-delà de la simple présence de deux vues. Les nouveaux tests de continuité et de découpage sont en cours de validation.

## Compatibilité

Aucune nouvelle table, migration, règle RLS, configuration Auth ou inscription d’appareil. Coordonnées dans readerAnnotations.points et readerInk.outlines existants ; préférences dans reader.tablet existant. Les traits de la note manuscrite restent normalisés dans leur bloc-notes, indépendamment des marges de page. Les anciennes annotations restent lisibles ; la présence de traits en marge maintient l’espace nécessaire même quand le mode tablette est désactivé. En double page les données ne sont pas supprimées, seuls le rendu des annotations et les outils d’édition sont désactivés. Ouvrir une note depuis le cahier rétablit une seule page.

Les corrections React Native du responder, d’offsetX/offsetY et des WebViews voisines n’introduisent aucun JavaScript dans AndroidCoran : le lecteur utilise déjà des événements MotionEvent et un Canvas natif par page. Le doigt n’est accepté que sur autorisation explicite ; le défilement du parent est suspendu pendant un trait. Aucun support matériel universel ni rejet de paume n’est revendiqué.

Les marges entourent la page originale ; elles ne redistribuent pas les mots, ne changent aucune ligne et ne modifient aucune police coranique. La typographie renforcée de l’interface du 19 novembre est conservée. L’export ajoute les marges en mode tablette ou si un trait les utilise ; les commentaires séparés ne deviennent pas une annexe PDF.

Les fonctionnalités antérieures de la branche Roqya, non fusionnée dans main et non modifiée depuis le dernier audit, restent dans le suivi de parité. Cette mise à jour ne prétend pas achever la totalité de la mission initiale.

## Validation

Code final vérifié : 599d641252ec319e930c5a679633e39e06cc4386. La compilation finale, Lint et les 190 tests JVM ont réussi dans [Actions](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44909139212). Les [suites finales Android 15](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44909139266) ont réussi : 53 tests téléphone et 26 tests tablette. Ces résultats incluent la continuité du surlignage entre les mots QCF et images, les espaces conservés entre lignes et la protection du Canvas voisin. Les premiers résultats ci-dessous précèdent ces derniers correctifs.

- [Compilation, Lint, APK et tests JVM](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44906911958) réussis : 190 JVM, zéro échec et zéro test ignoré. Compilation locale complète également réussie.
- [Suites Android 15](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44906911956) réussies : 50 tests téléphone et 23 tests tablette, zéro échec, erreur ou test ignoré. Rapports XML/HTML téléchargés et comptes vérifiés.
- Nouveaux contrôles : coordonnées négatives conservées après sérialisation, dimensions des marges, capture d’un trait dans la marge et pixels du PNG exporté, jaune prioritaire sur difficulté puis restitution de la teinte, doigt explicitement autorisé dans page et note, restauration de cette autorisation, outils d’annotation en plein écran après le mode Sélection.
- Les tests précédents restent exécutés : comptes locaux séparés, persistance/suppression, apprentissage et révision plein écran, double page RTL et autres écrans. Les suites d’interface utilisent les animations désactivées et des événements synthétiques.

Testé indique ces scénarios, pas une recette exhaustive. L’action rapide réutilisant les notes a été contrôlée dans le code mais n’a pas de nouveau parcours UI spécifique ; les marges des éditions en images, toutes les transformations de zoom, tous les exports de 20 pages et tous les menus ne sont pas déclarés intégralement validés. Honor Pad 10, stylet réel, rejet de paume, récitations distantes et synchronisation réelle entre applications/comptes restent non validés. Aucune interaction avec un compte réel ni changement backend pendant les tests.

En local, les 26 tests tablette Android 15 ont réussi au format 1600×1000. Capture finale examinée : pages 2/1 visibles côte à côte dans le bon ordre RTL. Les 3 279 fichiers de référence sont inchangés (SHA-256), état Git original propre. Émulateur de test arrêté après restauration de sa taille et densité. Appareils physiques et synchronisation réelle restent non validés.

APK final Actions (code 599d641), SHA-256 : 4D0EC5659C08E7B8E3ACA2D2FE1E018241602B030CFFD7923F111D855476ECA6. Rapports XML finaux vérifiés : téléphone 53 et tablette 26, zéro échec/erreur/ignoré.

