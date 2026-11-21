# Actualisation Expo 0.9.40 / 0.9.41 — 21 novembre 2026

## Audit en lecture seule

Main Expo reste à 42dedf8079b26bf28ee87a530e82274bb5e1e87e. La branche feat/tablet-notebook passe de 22b3090 à [bb6b29588afa8d26c13127239a47bc9f1408e363](https://github.com/Msoumaya2019/coran-memoire/commit/bb6b29588afa8d26c13127239a47bc9f1408e363), après [147d5a5](https://github.com/Msoumaya2019/coran-memoire/commit/147d5a566091e03fb5e27476af6f67656d0a8795). Les branches Roqya et petites sourates n’ont pas changé. Comparaison GitHub et copies de consultation indépendantes dans work/reference-updates-2026-11-21 ; aucune écriture dans le clone de référence ni sur GitHub Expo.

| Origine | Fonction / données | Équivalent natif | Développement | Test |
|---|---|---|---|---|
| core/annotationWorkspace.ts, coranTest/inkLayer.ts, ui/NativeInkSurface.tsx | Marges blanches externes 35 % à gauche/droite et 8 % en haut/bas ; coordonnées relatives à la page, négatives admises | AnnotationWorkspace.kt, MushafView.kt, ReaderInk.kt | Implémenté | En développement |
| ui/ReaderPrintedPage.tsx, services d’export | Même repère pour rendu, sélection, gomme, persistance et PNG/PDF | MushafView.kt, AnnotationExport.kt | Implémenté | En développement |
| ui/ReaderAnnotationEditor.tsx, ui/NoteHandwriting.tsx | Autorisation explicite du doigt ou d’un stylet non reconnu dans le commentaire ; capture du geste | ReaderInkPanel.kt, HandwritingView.kt | Implémenté | En développement |
| App.tsx, core/tablet.ts | Bouton Doigt persistant, activation de l’annotation et des outils | TabletReader.kt, reader.tablet.stylusOnly | Implémenté | En développement |
| core/tablet.ts, App.tsx | Annotation en plein écran simple page ; outils flottants, accès au doigt | TabletReaderControls, TabletReader.kt | Implémenté | En développement |
| App.tsx, core/tablet.ts | Double page agrandie et jointe, plein écran en paysage, lecture seule ; notes masquées mais conservées ; préférence portrait avec indication de rotation | Tablet.kt, TabletReader.kt | Implémenté | En développement |
| MushafPage.tsx, coranTest/html.ts | Audio jaune prioritaire sur difficulté/marque-page, glyphes inchangés | MushafView.kt, TajwidReader.kt | Implémenté | En développement |
| App.tsx | Actions rapides réutilisant la note existante d’un mot/verset et conservant commentaire/écriture ; formes rapides sans ouverture forcée de l’éditeur | TabletReader.kt | Implémenté | En développement |

## Compatibilité

Aucune nouvelle table, migration, règle RLS, configuration Auth ou inscription d’appareil. Coordonnées dans readerAnnotations.points et readerInk.outlines existants ; préférences dans reader.tablet existant. Les traits de la note manuscrite restent normalisés dans leur bloc-notes, indépendamment des marges de page. Les anciennes annotations restent lisibles ; la présence de traits en marge maintient l’espace nécessaire même quand le mode tablette est désactivé. En double page les données ne sont pas supprimées, seuls le rendu des annotations et les outils d’édition sont désactivés. Ouvrir une note depuis le cahier rétablit une seule page.

Les corrections React Native du responder, d’offsetX/offsetY et des WebViews voisines n’introduisent aucun JavaScript dans AndroidCoran : le lecteur utilise déjà des événements MotionEvent et un Canvas natif par page. Le doigt n’est accepté que sur autorisation explicite ; le défilement du parent est suspendu pendant un trait. Aucun support matériel universel ni rejet de paume n’est revendiqué.

Les marges entourent la page originale ; elles ne redistribuent pas les mots, ne changent aucune ligne et ne modifient aucune police coranique. La typographie renforcée de l’interface du 19 novembre est conservée. L’export ajoute les marges en mode tablette ou si un trait les utilise ; les commentaires séparés ne deviennent pas une annexe PDF.

Les fonctionnalités antérieures de la branche Roqya, non fusionnée dans main et non modifiée depuis le dernier audit, restent dans le suivi de parité. Cette mise à jour ne prétend pas achever la totalité de la mission initiale.

## Validation

Compilation initiale réussie. Tests des nouvelles règles et parcours Android en cours. Honor Pad 10, stylet réel, enregistrements audio réels et synchronisation entre applications/comptes non validés ; aucune interaction avec un compte réel pendant les tests.
