const fs=require('fs'),path=require('path'),crypto=require('crypto');
const reference=process.argv[2];if(!reference) throw new Error('Usage: node scripts/verify-reference.cjs <independent-read-only-reference>');
const manifest=JSON.parse(fs.readFileSync(path.join(__dirname,'../docs/source-manifest.json'),'utf8'));
const changed=[];
for(const entry of manifest.files) { const file=path.join(reference,entry.path);if(!fs.existsSync(file)||crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')!==entry.sha256) changed.push(entry.path); }
console.log(JSON.stringify({commit:manifest.commit,checked:manifest.files.length,changed}));if(changed.length) process.exitCode=1;
