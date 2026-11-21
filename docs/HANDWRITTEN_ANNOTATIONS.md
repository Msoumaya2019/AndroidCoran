# Annotations et cahier du Mushaf

1. Dans Réglages → Affichage et appareil, laisser Mode tablette sur Automatique sur une tablette, Les outils sont aussi accessibles sur téléphone par le menu ⋯ → Annoter.
2. Sur tablette, Lecture masque les outils et Annoter les affiche en bas. Mes notes ouvre la liste et Révisions le cahier existant. En simple page, toucher Annoter puis choisir Stylo, Surligneur, Entourer, Souligner, Flèche, Gomme ou Ajouter une note. Les marges blanches permettent d’écrire autour de la page sans déplacer les versets.
3. Stylet uniquement est activé par défaut. Le bouton Doigt autorise explicitement un doigt ou un stylet déclaré comme contact tactile et active l’annotation. Aucune bascule silencieuse n’est effectuée.
4. Lever le stylet sauvegarde les tracés ordinaires. Ajouter une note ouvre un brouillon dans une fenêtre flottante ; sa poignée permet de la déplacer. Le titre est modifiable. Les modes Stylet et Clavier partagent une grande zone blanche et les deux contenus sont conservés ensemble.
5. Si le stylet ne fonctionne pas dans la fiche, utiliser Autoriser le doigt ou un stylet non reconnu. Cette autorisation locale est distincte de la préférence de la page. Les modifications de la note restent en brouillon jusqu’à Enregistrer ; Annuler le trait et Effacer la note permettent de corriger l’écriture.
6. Choisir au moins une catégorie et la couleur indépendante de l’icône, puis Enregistrer. La sauvegarde locale est attendue avant fermeture automatique et retour à Lecture, sans changer le zoom ni le déplacement. Les petits formats gardent les métadonnées dans un dialogue séparé. Une catégorie personnelle peut être créée. Masquer catégories et couleur agrandit l’espace blanc. Fermer un brouillon modifié propose l’enregistrement ou l’abandon. Supprimer l’annotation puis confirmer retire le repère et les commentaires.
7. Les icônes de bloc-notes agrandies dans la marge ouvrent les notes enregistrées en lecture seule. Modifier donne accès aux outils ; la fenêtre se règle à la taille du contenu et peut être déplacée/redimensionnée avec sa poignée et son coin inférieur droit. Agrandir et Réduire proposent aussi des tailles rapides. Plusieurs notes peuvent concerner le même verset. Un appui simple sur le texte ne sélectionne rien ; maintenir le doigt ouvre les options avec vibration système si disponible.

Les marges initiales représentent 35 % de la largeur originale à gauche/droite et 8 % de la hauteur en haut/bas. Toute la surface visible accepte les traits, au-delà de ces marges si la fenêtre est plus large. Le zoom conserve le papier sous les deux doigts ; les mêmes coordonnées servent aux tracés et à la gomme. Les traits conservent leurs coordonnées relatives à la page, même hors de son rectangle. Ils sont conservés après changement de page et réouverture, et exportés. Un trait existant en marge maintient cet espace même si le mode tablette est ensuite désactivé.

Un appui long affiche une barre flottante proche du passage, sans réduire la page : Écouter, Difficile, Note, Enregistrer ma récitation et X. Le bouton d’enregistrement est directement visible et ouvre le panneau existant pour le seul verset choisi. Le menu ⋯ conserve les outils du mot/verset, les formes, la répétition et le marque-page. Les formes rapides mettent à jour la note existante du passage et conservent ses commentaires. Sur les éditions en images, le verset est disponible ; Choisir un mot dans le Coran avec Tajwid ouvre le même passage dans l’édition possédant ses positions de mots.

Mon cahier de révision regroupe les éditions, avec filtres, catégories et tris. Favori et Passage maîtrisé sont propres à la note : ils ne valident pas un apprentissage. Marquer difficile réutilise le marqueur existant de révision. Ouvrir une note rétablit une page et sa bonne édition.

## Plein écran et double page

Sur tablette, les deux lignes compactes (navigation puis Lecture/Annoter/Mes notes/Révisions) sont masquées en plein écran ; le bouton de sortie les rétablit. Activer Annoter avant le plein écran conserve les outils en bas. Lecture les masque sans effacer les tracés. Options regroupe français, double page, doigt, affichage, export, audio et programme. L’interface smartphone conserve ses commandes antérieures. Le verset courant de la récitation est signalé par une bande jaune continue par ligne, sans coupure entre les mots, prioritaire sur les autres repères et sans modifier les glyphes.

Double page affiche la page paire à gauche et l’impaire à droite, centrées et séparées par une gouttière blanche de 24 dp. En paysage, son activation passe en plein écran ; en portrait la préférence est conservée avec une invitation à tourner la tablette. Ce mode est réservé à la lecture : les annotations restent visibles ; les outils de dessin sont réservés à une seule page.

Exporter propose un PNG de la page courante ou un PDF de 1 à 20 pages avec les marges nécessaires. Le cahier exporte le texte des notes filtrées ou cochées. Les commentaires séparés ne sont pas ajoutés au PDF ; aucune reconnaissance de l’écriture manuscrite n’est effectuée.

La transition native de page utilise une courbure de feuille ombrée. Réglages → Affichage et appareil → Animation de page courbée permet de la désactiver.

## Conservation et validation

readerAnnotations et reader.tablet restent dans le JSON utilisateur existant ; les anciennes notes readerInk sont conservées. Chaque compte et l’invité ont une copie locale distincte. Aucun changement de backend.

Le [rapport des notes et de l’en-tête tablette](TABLET_CHROME_AND_NOTES_2026-11-21.md) donne les résultats exacts. Les événements stylet/doigt sont synthétiques sur émulateur. Honor Pad 10, précision et rejet de paume avec un stylet physique, récitations distantes réelles et synchronisation entre appareils restent à vérifier.

Le canevas manuscrit conserve des proportions fixes (noteAspect pour les nouvelles notes). Redimensionner ajoute au besoin des bandes grises autour du papier, sans étirer les traits. Sans proportions enregistrées, les notes anciennes utilisent le format historique 1,4 ; leurs coordonnées ne sont pas réécrites.
