# AndroidCoran

Application Android native Kotlin / Jetpack Compose, indépendante de l'application Expo.

**Portage en cours : la parité complète n'est pas atteinte.** Consulter [FUNCTIONAL_PARITY.md](FUNCTIONAL_PARITY.md), [docs/VALIDATION.md](docs/VALIDATION.md) et les inventaires de référence avant toute utilisation en production.

## Compilation

JDK 17 ou plus récent, SDK Android 36 et Gradle Wrapper inclus :

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

L'APK debug est généré dans `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions publie cet APK après chaque push sur main. L'exécution des tests sur appareil nécessite un émulateur ou téléphone ; elle est distincte des tests JVM.

## Backend existant

Le projet reste `https://npbwnvrqmajwqtnncuyv.supabase.co`. Saisir sa **clé publique publishable/anon** dans Réglages. Aucun secret `service_role`, mot de passe ou clé privée n'est nécessaire à la compilation. Aucun SQL de migration n'est exécuté par cette application.

Les documents `user_state.data` conservent leurs champs JSON inconnus, pour éviter de supprimer des propriétés ajoutées par Expo ou Swift. Les sauvegardes locales sont séparées par compte. La fusion à trois versions reprend les règles Expo : champs locaux explicites, historiques réunis, séances terminées conservées et avancement maximal. Une modification concurrente survenant pendant la sauvegarde suspend la publication ; les données locales restent conservées. Une base distante absente ou une identité incompatible suspend aussi la fusion. La connexion et la fusion contre le serveur réel restent non validées.

La connexion réutilise les comptes Auth existants. La clé réelle est absente de la référence : la connexion et les politiques RLS doivent être validées avec un compte de test avant toute diffusion.

## Ressources coraniques

Les données Hafs, métadonnées, traduction française, pages, coordonnées et glyphes sont copiés de la référence immuable. Les images sont conservées sans retouche. Les polices WOFF2 ont été converties en TTF pour Canvas Android ; la fidélité aux polices COLR et aux repères doit encore être comparée sur appareils. Conserver les licences et attributions ; `app/src/main/assets/TANZIL-LICENSE.txt` est inclus.

Toutes les pages de deux éditions sont embarquées pour l'accès hors connexion. L'APK est volumineux : préparer Play Asset Delivery ou des téléchargements vérifiés avant publication Google Play.

Les récitations publiques utilisent un cache Media3 de 250 Mo, évacuant les fichiers les moins récemment utilisés. Les portions réellement téléchargées peuvent être relues hors connexion ; écouter un début de sourate ne télécharge pas automatiquement sa totalité. Android peut vider ce cache. Les URL signées des enregistrements personnels restent hors de ce cache partagé. Le téléchargement audio explicite et la reprise après arrêt du processus restent à porter.

Les objectifs et connaissances peuvent sélectionner une sourate, un hizb, un juz ou une plage de versets globaux. La contrainte de volume minimal du programme est conservée. Les traductions incluent leurs notes originales et permettent de sélectionner un verset. Les marques-pages conservent les dates d'utilisation et les marqueurs de suppression ; la synchronisation distante de ces marqueurs reste à valider avec un compte réel.

## Production et signature

`./gradlew assembleRelease bundleRelease` prépare les livrables non signés. Ajouter ultérieurement une configuration de signature injectée depuis un stockage sécurisé ou les secrets de CI ; ne jamais versionner de keystore ni mots de passe. La configuration Firebase/FCM et le routage push partagé nécessitent une adaptation additive autorisée (voir `docs/FCM_PROPOSAL.md`).

## Sécurité des dépôts

L'historique est créé de zéro. L'unique remote de push doit être `https://github.com/Msoumaya2019/AndroidCoran.git`. Le dépôt Expo est une source de lecture ; sa copie d'audit reste hors du dépôt Android. Aucun dépôt Swift n'est utilisé pour le développement.

## Portage en cours : apprentissage, quiz et synchronisation différée

Les écrans natifs présentent désormais les statistiques par jour/semaine/mois, la reprise des séances partielles et leur historique. Le quiz propose les défis de 5/10 questions, les séries thématiques, les scores et corrections confirmés par le serveur. Les réponses quotidiennes hors connexion et les signalements avec capture sont sauvegardés dans une file SQLite par compte, reprise par WorkManager avec connexion réseau.

Les récitations distinguent le Coran des invocations ; les enregistrements locaux et les retours vocaux sont accessibles via Media3. L'administration permet la recherche paginée des comptes et l'historique des notifications sous le rôle Supabase existant. L'édition administrative et plusieurs parcours restent à porter : consulter FUNCTIONAL_PARITY.md. La parité complète n'est pas atteinte.

Validation locale : 32 tests JVM réussis et huit tests Android réussis sur Android 15. Les accès aux comptes existants et aux RPC nécessitent encore la clé publique Supabase ; ils ne sont pas annoncés comme validés.
