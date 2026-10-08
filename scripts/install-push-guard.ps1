$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskRemote=git -C $taskRoot remote get-url --push origin
if($taskRemote -ne 'https://github.com/Msoumaya2019/AndroidCoran.git') { throw 'Destination de push incorrecte' }
$taskHook=Join-Path $taskRoot '.git/hooks/pre-push'
$taskText=@'
#!/bin/sh
if [ "$2" != "https://github.com/Msoumaya2019/AndroidCoran.git" ]; then
  echo "Push refusé : destination différente d'AndroidCoran." >&2
  exit 1
fi
'@
[System.IO.File]::WriteAllText($taskHook,$taskText.Replace("`r`n","`n")+"`n",[System.Text.UTF8Encoding]::new($false))
