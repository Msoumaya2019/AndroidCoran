# Mise à jour du lecteur et du mode tablette — 19 novembre 2026

## Sources consultées en lecture seule

- Branche principale Expo : [42dedf8079b26bf28ee87a530e82274bb5e1e87e](https://github.com/Msoumaya2019/coran-memoire/commit/42dedf8079b26bf28ee87a530e82274bb5e1e87e), après ef541f3 : annotations tablette, transitions de page, notes sur mot/verset et repères persistants.
- Branche Expo [feat/tablet-notebook, 22b30901461ef84dd2566fb3dec2943a63f24f50](https://github.com/Msoumaya2019/coran-memoire/tree/22b30901461ef84dd2566fb3dec2943a63f24f50) : cahier, véritable disposition tablette, outils réservant leur propre espace, stylet uniquement et protection contre le changement de page pendant un trait. Les corrections d’intégration Hermes concernent Expo et n’introduisent aucun moteur JavaScript dans Android.
- Copies de consultation téléchargées dans un répertoire indépendant ; aucun fetch, fichier, branche ou commit ajouté au dépôt de référence original.

## Fonctionnalités portées

| Origine Expo | Rôle et données existantes | Équivalent Kotlin | Développement | Tests |
|---|---|---|---|---|
| src/core/tablet.ts, src/ui/TabletSettings.tsx | Mode auto/on/off, reader.tablet ; seuils réels de fenêtre | domain/Tablet.kt, ui/TabletSettings.kt | Implémenté | Testé partiellement (voir limites) |
| src/App.tsx, src/ui/TabletToolbar.tsx | Outils verticaux 72 dp, notes 280 dp, outils horizontaux réservés en portrait ; double page RTL | ui/TabletReader.kt | Implémenté | Testé partiellement (voir limites) |
| src/core/tabletInk.ts, src/ui/NativeInkSurface.tsx | Stylo/crayon, surligneur, cercle, souligné, flèche, gomme, sélection, note, six couleurs et épaisseurs ; historique 40 actions | MushafView.kt, ReaderInkDrawing.kt, TabletReader.kt | Implémenté | Testé partiellement (voir limites) |
| src/core/readerAnnotations.ts, src/ui/ReaderAnnotationMarks.tsx | Appui long mot/verset et repères résolus contre la géométrie originale, readerAnnotations | MushafView.kt, TajwidReader.kt, ReaderInk.kt | Implémenté | Testé partiellement (voir limites) |
| src/ui/ReaderAnnotationEditor.tsx | Texte et écriture manuscrite conservés ensemble, suppression | ReaderInkPanel.kt, HandwritingView.kt | Implémenté | Testé partiellement (voir limites) |
| src/ui/AnnotationNotebook.tsx, src/core/tablet.ts | Cahier : filtres sourate/page/couleur/type/favori/difficulté, tris, maîtrise indépendante des connaissances, réouverture de la bonne édition | AnnotationNotebook.kt (UI et domaine) | Implémenté | Testé partiellement (voir limites) |
| src/core/annotationExport.ts, src/services/annotationExport.ts, src/ui/AnnotationExportDialog.tsx | PNG page courante, PDF 1–20 pages et texte des notes filtrées | PrintedPage.kt, AnnotationExport.kt, FileProvider privé | Implémenté | Testé partiellement (voir limites) |
| src/ui/ReaderPageTurn.tsx | Transition de page 420 ms, mouvement réduit | AnimatedContent natif, réglage d’animation Android | Implémenté | Testé partiellement (voir limites) |

Tous les champs restent dans user_state.data existant. Les marqueurs de difficulté réutilisent difficultyMarkers et les préférences reader.tablet. Aucune migration, table, règle RLS, configuration Auth ou configuration d’appareil modifiée. Les trois parcours lecture/apprentissage/révision utilisent le même lecteur.

## Remplacement de l’ancienne adaptation Android

La navigation latérale, l’accueil à deux colonnes et la grille de sourates ajoutés le 18 novembre sont retirés. Les onglets de navigation de référence sont conservés ; l’adaptation tablette appartient désormais au lecteur. Seuils identiques à la source : outils/notes latéraux en paysage dès 1000 × 480 dp, double page en paysage si le centre conserve 640 dp et la hauteur 460 dp. La page paire est physiquement à gauche et la page impaire à droite, même si le système est RTL. Une fenêtre trop étroite revient à une page.

La lecture plein écran masque outils et notes ; les repères demeurent visibles. Le bouton Quitter la lecture plein écran rétablit les outils. Les commandes audio, la validation d’apprentissage et les notes de révision restent accessibles par le bouton habituel des commandes.

## Compatibilité des annotations

Le champ canonique Expo readerAnnotations est lu et écrit avec points, handwriting, anchor, text, width, penStyle, favorite et masteredAt. Les anciennes annotations Android readerInk sont conservées et dédupliquées par identifiant ; leurs coordonnées et notes ne sont pas effacées. Une copie locale indépendante par compte protège les champs absents d’un ancien état distant. Les dates du nouveau format sont ISO avec millisecondes.

La fusion canonique suit la référence : horodatage récent gagnant, suppression gagnante à égalité, puis départage JSON déterministe. Une ancienne sauvegarde ne peut écraser une suppression ; une annulation explicite utilise une nouvelle date et peut restaurer le repère. Le champ hérité readerInk conserve son comportement historique de tombstone prioritaire. Cela diffère volontairement de l’ancienne règle globale Android « toute suppression gagne même contre une modification plus récente ».

## Typographie

Interface Android sans empattements, famille système Roboto/SansSerif, corps 16–17 sp Medium, petits textes 14 sp, titres renforcés Bold. Les dimensions suivent le réglage de taille de police Android. Les fontes QCF/Amiri, les glyphes, lignes, retours et proportions du Mushaf ne sont pas modifiés. Aucune nouvelle police distante n’est nécessaire au démarrage.

## Adaptations et limites

- Les notes natives sont sauvegardées après chaque modification/trait terminé ; il n’y a pas de bouton Enregistrer ni de brouillon non enregistré à confirmer.
- La transition native est un glissement/fondu, plutôt que la simulation web de feuille en trois dimensions.
- Les éditions en images n’inventent pas de coordonnées de mots : l’action dédiée ouvre le même verset dans QCF. Les anciennes éditions Android sont conservées.
- Les exports capturent le Canvas original avec ses tracés ; les commentaires ne sont pas ajoutés au PDF. Le texte exporté ne fait aucune reconnaissance de l’écriture manuscrite.
- Le mode stylet uniquement est activé par défaut pour la page et la note. Le doigt conserve la navigation/zoom. La pression est prise en charge nativement ; la précision et le rejet de paume d’un matériel réel restent à vérifier.
- Aucune connexion avec un compte réel ni synchronisation Expo/Android sur deux appareils n’est déclarée validée. Les tests locaux utilisent un invité et des ressources locales.
- Le portage de cette mise à jour ne signifie pas parité complète avec toutes les fonctionnalités anciennes ou toutes les branches Expo (notamment Roqya/FCM).

## Validation

Code applicatif : 5ef6d241477cfd4ed0cfcaeb25216a8627ef839a. [Compilation GitHub Actions réussie](https://github.com/Msoumaya2019/AndroidCoran/actions/runs/44564107331) : 185 tests JVM, zéro échec/ignoré, Lint, APK et compilation des tests Android. Les rapports HTML ont été récupérés. Compilation et Lint locaux également réussis.

Sur l’émulateur Android 15 local : 19 tests réussis en paysage 1600 × 1000 à 160 dpi, 19 en portrait 1000 × 1600 à 160 dpi, puis 47 sur le format téléphone natif 1080 × 2400 à 420 dpi. Aucune suppression des notes existantes dans les fixtures : les tests nettoient seulement les identifiants qu’ils créent. Les parcours comprennent la recréation, la réouverture/suppression d’une note, le cahier et sa maîtrise indépendante des connaissances, la double page, les panneaux, le plein écran, les pixels du PNG, deux pages PDF, les coordonnées de mots et le rejet des événements doigt en mode stylet uniquement.

Les premiers passages ont révélé des cibles de test ambiguës/masquées par Material, des sauvegardes asynchrones non attendues et une accumulation des notes de test, puisque leur copie locale est durable. Les tests utilisent maintenant le dispatcher Compose standard et ciblent précisément l’onglet ou le titre de note. Deux courses observées dans les suites complètes sont en cours de vérification : action de navigation pendant le chargement des contenus d’accueil et publication distincte du verset/progrès Media3. Les assertions de plein écran, d’apprentissage et de progression audio sont conservées.

APK de ce code : artefact AndroidCoran-debug-apk du build 44564107331 ; SHA-256 4B76894FD5DFA79F0D78B1C99DF61B305DC3CB6362C3969B398E7C2BE616C9ED.

La suite CI finale sera consignée après son résultat. Les menus de tous les types d’annotations, le rendu de tous les mots dans toutes les éditions, tous les tris du cahier, le partage vers des applications externes et l’animation 420 ms avec animations système activées ne sont pas déclarés intégralement validés. Les tests d’interface désactivent les animations et l’aide système de première ouverture plein écran.
