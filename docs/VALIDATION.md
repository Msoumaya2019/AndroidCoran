# Vérifications

Ce document doit être mis à jour avec les résultats réels ; aucune fonctionnalité distante n'est validée sans connexion réelle.

- Compilation locale : Testé — `assembleDebug` réussi, APK debug produit.
- Tests JVM : Testé — 11 tests, 0 échec.
- Android Lint : Testé — `lintDebug` réussi ; les avertissements ne constituent pas une validation de tous les parcours.
- Tests instrumentés sur émulateur : Testé — 4 tests, 0 échec, AVD coran Android 15.
- Authentification d'un compte existant : Bloqué — clé publique Supabase absente.
- Synchronisation et RLS : Bloqué — pas de compte de test connecté.
- Audio réseau : Testé — un verset Alafasy réellement lu par Media3, troisième répétition atteinte sur l'émulateur. Autres récitateurs, passages continus, arrière-plan et interruptions : Non commencé pour la validation sur appareil.
- Fidélité visuelle de toutes les éditions : Non commencé.
- FCM : Bloqué — Firebase et adaptation serveur additive à autoriser.

Les tests automatisés couvrent les règles de répétition, les 6236 versets/604 pages, l'ordre depuis An Nâs, les jours sélectionnés, les validations partielles, la préservation des champs JSON, la partition des révisions et les étapes de consolidation.

Limites connues : outils administrateur, avatar, téléchargements audio, fusion hors connexion complète, push FCM et fonctions sociales avancées restent à porter. L'enregistrement natif, la lecture continue, les rappels locaux et les services Supabase sont codés mais ne sont pas validés intégralement. Le document de parité recense leurs sources.

Contrôle de la référence : 3279 empreintes SHA-256 identiques au relevé initial ; checkout d'audit propre ; HEAD/main et toutes les branches/refs distantes Expo identiques avant et après les opérations. Aucun outil d'écriture n'a ciblé Expo, Supabase ni Swift. Le dépôt Swift n'a pas été consulté : absence d'intervention, et non audit de son état.

APK avec toutes les ressources embarquées : environ 377 Mo. Il s'agit d'un build de développement ; ni signature production ni publication Google Play n'ont été effectuées.

GitHub Actions : **Bloqué**. Le run `37758520117` a échoué avant toute étape et sans runner : GitHub indique des paiements récents du compte en échec ou un plafond de dépenses à augmenter. Aucun changement de facturation n'a été effectué. Le workflow devra être relancé après résolution côté compte. Un APK compilé localement peut être distribué en préversion privée dans le nouveau dépôt.
