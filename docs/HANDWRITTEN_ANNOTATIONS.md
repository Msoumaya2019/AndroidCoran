# Annotations et cahier du Mushaf

1. Dans Réglages → Affichage et appareil, laisser Mode tablette sur Automatique sur une tablette, ou choisir Activé pour disposer des outils sur un téléphone. Stylet uniquement est activé par défaut ; le désactiver permet de dessiner au doigt.
2. Ouvrir le Coran depuis Lecture, Apprentissage ou Révision. Toucher Annoter puis sélectionner Stylo pour entourer librement sans reconnaissance de forme. Les outils offrent aussi cercle, surligneur, souligné, flèche, gomme et note. Couleur et épaisseur règle les six couleurs, l’encre/crayon et les largeurs.
3. Le tracé est sauvegardé en levant le stylet. Toucher le repère ou Mes annotations pour ajouter une note. L’outil Ajouter une note ouvre directement sa fiche.
4. Dans Stylet, écrire à la main dans la zone blanche ; chaque trait terminé est sauvegardé. Annuler le trait corrige le dernier trait. Le commentaire clavier peut coexister avec les traits dans l’onglet Commentaire.
5. Fermer conserve le repère. Supprimer l’annotation puis confirmer retire le repère et les deux commentaires.

Un appui long sur un mot QCF ou de la lecture simplifiée propose les actions du mot ou du verset entier, l’écoute, la répétition et la difficulté. Sur les éditions en images, le verset est disponible ; Choisir un mot dans le Coran avec Tajwid ouvre le même passage dans l’édition possédant les coordonnées de mots.

Mon cahier de révision regroupe toutes les éditions, avec filtres et tris. Favori et Passage maîtrisé sont propres à la note : ils ne valident pas automatiquement un apprentissage. Marquer difficile réutilise le marqueur existant de révision. Toucher une note rouvre sa page et son édition.

En paysage suffisamment large, les outils et les annotations occupent leurs panneaux dédiés. En portrait, les outils sont sous la page et les notes dans un panneau flottant. Double page place la page impaire à droite. Lecture plein écran masque les panneaux en conservant les tracés ; Quitter la lecture plein écran les rétablit. Le bouton habituel Afficher les commandes donne toujours accès à l’audio et aux validations.

Exporter propose un PNG de la page courante ou un PDF de 1 à 20 pages. Le cahier peut exporter le texte des notes filtrées ou cochées. Aucune conversion automatique des traits en texte n’est effectuée.

## Conservation et validation

Les champs readerAnnotations et reader.tablet utilisent le JSON utilisateur existant. Les anciennes notes readerInk sont conservées. Chaque compte et l’invité ont leur copie locale distincte. Aucun changement du backend n’est nécessaire.

La fusion du nouveau format suit les dates comme Expo ; une suppression empêche une ancienne sauvegarde de rétablir la note et une annulation explicite peut la restaurer avec un nouvel horodatage. La synchronisation entre appareils et avec Expo doit encore être vérifiée avec un compte réel.

Les tests d’émulateur utilisent des événements stylet synthétiques. Le confort, la précision et le rejet de paume avec un stylet physique restent à vérifier.
