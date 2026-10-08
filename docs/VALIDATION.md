# Vérifications

Ce document doit être mis à jour avec les résultats réels ; aucune fonctionnalité distante n'est validée sans connexion réelle.

- Compilation locale : Testé — `assembleDebug` réussi, APK debug produit.
- Tests JVM : Testé — 18 tests, 0 échec.
- Android Lint : Testé — `lintDebug` réussi ; les avertissements ne constituent pas une validation de tous les parcours.
- Tests instrumentés sur émulateur : Testé — 6 tests, 0 échec, AVD coran Android 15 : navigation, ressources, isolation des comptes, audio répété, cache après disparition de la source et sélecteur hizb.
- Authentification d'un compte existant : Bloqué — clé publique Supabase absente.
- Synchronisation et RLS : Bloqué — pas de compte de test connecté.
- Audio réseau : Testé — un verset Alafasy réellement lu par Media3, troisième répétition atteinte sur l'émulateur. Autres récitateurs, passages continus, arrière-plan et interruptions : Non commencé pour la validation sur appareil.
- Fidélité visuelle de toutes les éditions : Non commencé.
- FCM : Bloqué — Firebase et adaptation serveur additive à autoriser.

Les tests automatisés couvrent les règles de répétition, les 6236 versets/604 pages, l'ordre depuis An Nâs, les jours sélectionnés, les validations partielles, la préservation des champs JSON, la partition des révisions et les étapes de consolidation.

Limites connues : outils administrateur, avatar, téléchargements audio, files hors connexion des services sociaux, push FCM et fonctions sociales avancées restent à porter. L'enregistrement natif, la lecture continue, les rappels locaux et les services Supabase sont codés mais ne sont pas validés intégralement. Le document de parité recense leurs sources.

Contrôle de la référence : 3279 empreintes SHA-256 identiques au relevé initial ; checkout d'audit propre ; HEAD/main et toutes les branches/refs distantes Expo identiques avant et après les opérations. Aucun outil d'écriture n'a ciblé Expo, Supabase ni Swift. Le dépôt Swift n'a pas été consulté : absence d'intervention, et non audit de son état.

APK avec toutes les ressources embarquées : environ 377 Mo. Il s'agit d'un build de développement ; ni signature production ni publication Google Play n'ont été effectuées.

GitHub Actions : **Testé** — run public `37761443828` réussi : tests JVM, Lint, compilation et artefacts APK/rapports publiés. Le passage en public a levé le blocage de facturation du premier run. Le SDK utilise les paquets explicites Android 36 et build-tools 35.0.0. Aucun paramètre de facturation modifié.
Suite du portage natif : règles des marques-pages et notes françaises testées en JVM ; sélection personnalisée sourates/hizb/juz/plages et niveaux de connaissance intégrés. Les choix du programme sont sauvegardables par Compose ; la rotation réelle de ce parcours reste à valider. Le cache audio public Media3 est limité à 250 Mo ; la lecture des octets en cache après suppression de la source a été réellement testée sur Android. Les URL signées personnelles ne passent pas par ce cache.

Fusion offline : portage de `src/core/offlineMerge.ts`, avec quatre tests JVM couvrant les champs indépendants/inconnus, les historiques, l’avancement maximal, les séances terminées, les suppressions explicites, la remise à zéro et les comptes distincts. La mise à jour distante conserve le filtre `updated_at` et ne supprime pas la sauvegarde locale en cas de conflit. Authentification, RLS et fusion avec le serveur réel : Bloqué, clé publique et compte de test absents.
Portage social et contenus : 22 tests JVM réussis, dont quatre contrats supplémentaires (cible privée/groupe, longueur des messages, identité du propriétaire et payload d’invocation sans faux versets). Les six tests Android précédents ne valident pas ces nouveaux parcours distants. Les écrans compilent ; aucun compte Supabase réel n’est connecté. Pagination, Realtime, lecture des messages, rôles de groupe, rendez-vous, favoris, avatars et contact administrateur : implémentations non validées côté serveur. Le stockage des photos utilise le bucket original friend-avatars ; sélection/cadrage/orientation sur appareil et photo avant inscription restent à vérifier ou compléter.
