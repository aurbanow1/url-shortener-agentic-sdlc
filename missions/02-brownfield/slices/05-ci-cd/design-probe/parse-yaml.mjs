// Parses each YAML file named on the command line and prints it as JSON, failing on any parse error
// or warning. YAML 1.2 (the `yaml` package's default), so a workflow's `on:` key stays the string "on".
// The `yaml` package comes from the installed OpenRig CLI, so no network or install is needed.
// Usage: node parse-yaml.mjs <path to node_modules/yaml> <file.yml>...
import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';

const [yamlModule, ...files] = process.argv.slice(2);
const { parseDocument } = createRequire(import.meta.url)(yamlModule);
let failed = false;
for (const file of files) {
  const doc = parseDocument(readFileSync(file, 'utf8'), { prettyErrors: true });
  const problems = [...doc.errors, ...doc.warnings];
  for (const p of problems) console.log(`${file}: ${p.message}`);
  if (problems.length) failed = true;
  else console.log(`${file}: parsed\n${JSON.stringify(doc.toJS(), null, 2)}`);
}
process.exit(failed ? 1 : 0);
