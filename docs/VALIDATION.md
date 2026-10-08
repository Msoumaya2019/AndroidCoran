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

Portage apprentissage/statistiques/quiz : 32 tests JVM réussis le 8 octobre 2026, Lint sans erreur et APK compilé. Cinq tests statistiques couvrent le volume sans doublons, la semaine avec changement d'heure, l'extension des séances sans déplacer les dates et la consolidation. Le test de verset réparti sur deux pages utilise un index synthétique : ce n'est pas une validation visuelle du Mushaf. Cinq tests quiz couvrent la réponse quotidienne unique, les dates/réponses invalides, la confirmation serveur fusionnée, les réponses en attente exclues du score et les défis terminés/expirés.

La migration SQLite v1 vers v2 ajoute uniquement une file locale, avec propriétaire et identifiant idempotent. WorkManager attend le réseau et retente séparément les données d'apprentissage, le quiz et les signalements. Aucune migration Supabase appliquée. Connexion, RPC, pièces jointes et préférences de quiz sur serveur : Bloqué, clé publique absente.

Tests Android : **Testé** — huit tests exécutés et réussis sur l’émulateur coran Android 15. Les deux nouveaux tests vérifient la file persistante après réouverture, l’isolation des propriétaires et la migration SQLite v1→v2 sans perdre les états ou caches. Les six tests existants vérifient les ressources, la navigation/lecteur Compose, la sélection de hizb et le cache audio. Ces tests ne valident pas les RPC Supabase ni le microphone ou les photos réelles. L’écran de récitations a ensuite reçu l’écoute locale et les retours vocaux ; sa compilation est vérifiée séparément.

Administration : consultation native des comptes paginés/recherchés, volumes mémorisés, objectif, rythme et date de synchronisation ; historique des notifications. Accès conditionné à la table existante app_admins pour l'utilisateur connecté, vérifié avant chaque lecture administrative. Les politiques/RPC serveur restent l'autorité. Aucun compte réel administrateur testé. L'édition des quiz, contenus, corrections, rapports et l'envoi de notifications restent à porter.

CI précédente : run public 37767489071 réussi pour le portage social. Les écrans administrateur et de retours vocaux sont compilés, sans validation serveur. Les pièces jointes déjà mises en file sont conservées même si la planification du travail échoue ; les annulations de coroutines ne déclenchent pas un nouvel upload.
