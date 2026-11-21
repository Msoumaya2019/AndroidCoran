# Annotations et cahier du Mushaf

1. Dans Réglages → Affichage et appareil, laisser Mode tablette sur Automatique sur une tablette, ou choisir Activé pour disposer des outils sur un téléphone.
2. En simple page, toucher Annoter puis choisir Stylo, Surligneur, Entourer, Souligner, Flèche, Gomme ou Ajouter une note. Les marges blanches permettent d’écrire autour de la page sans déplacer les versets.
3. Stylet uniquement est activé par défaut. Le bouton Doigt autorise explicitement un doigt ou un stylet déclaré comme contact tactile et active l’annotation. Aucune bascule silencieuse n’est effectuée.
4. Lever le stylet sauvegarde le tracé. Toucher le repère ou Mes annotations ouvre la fiche ; Ajouter une note l’ouvre directement. L’écriture du commentaire est dans l’onglet Stylet, le texte dans Commentaire. Les deux peuvent coexister.
5. Si le stylet ne fonctionne pas dans la fiche, utiliser Autoriser le doigt ou un stylet non reconnu. Cette autorisation locale est distincte de la préférence de la page. Chaque trait terminé est sauvegardé ; Annuler le trait et Effacer la note permettent de corriger l’écriture.
6. Fermer conserve le repère. Supprimer l’annotation puis confirmer retire le repère et les commentaires.

Les marges représentent 35 % de la largeur originale à gauche/droite et 8 % de la hauteur en haut/bas. Les traits conservent leurs coordonnées relatives à la page, même hors de son rectangle. Ils sont conservés après changement de page et réouverture, et exportés. Un trait existant en marge maintient cet espace même si le mode tablette est ensuite désactivé.

Un appui long sur un mot QCF ou de la lecture simplifiée propose les actions du mot/verset, l’écoute, la répétition et la difficulté. Les formes rapides mettent à jour la note existante du passage et conservent ses commentaires. Sur les éditions en images, le verset est disponible ; Choisir un mot dans le Coran avec Tajwid ouvre le même passage dans l’édition possédant ses positions de mots.

Mon cahier de révision regroupe les éditions, avec filtres et tris. Favori et Passage maîtrisé sont propres à la note : ils ne valident pas un apprentissage. Marquer difficile réutilise le marqueur existant de révision. Ouvrir une note rétablit une page et sa bonne édition.

## Plein écran et double page

En plein écran simple page, Annoter et Autoriser le doigt restent accessibles ; les outils flottants apparaissent pendant l’annotation. Terminer les annotations les masque. Afficher les commandes conserve l’accès à l’audio et aux validations. Le verset courant de la récitation est signalé par une bande jaune continue par ligne, sans coupure entre les mots, prioritaire sur les autres repères et sans modifier les glyphes.

Double page affiche la page paire à gauche et l’impaire à droite, agrandies et jointes. En paysage, son activation passe en plein écran ; en portrait la préférence est conservée avec une invitation à tourner la tablette. Ce mode est réservé à la lecture : outils et annotations sont masqués, sans suppression des notes. Une seule page les retrouve.

Exporter propose un PNG de la page courante ou un PDF de 1 à 20 pages avec les marges nécessaires. Le cahier exporte le texte des notes filtrées ou cochées. Les commentaires séparés ne sont pas ajoutés au PDF ; aucune reconnaissance de l’écriture manuscrite n’est effectuée.

## Conservation et validation

readerAnnotations et reader.tablet restent dans le JSON utilisateur existant ; les anciennes notes readerInk sont conservées. Chaque compte et l’invité ont une copie locale distincte. Aucun changement de backend.

Le [rapport du 21 novembre](REFERENCE_UPDATES_2026-11-21.md) donne les résultats exacts. Les événements stylet/doigt sont synthétiques sur émulateur. Honor Pad 10, précision et rejet de paume avec un stylet physique, récitations distantes réelles et synchronisation entre appareils restent à vérifier.
