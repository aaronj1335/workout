#!/usr/bin/env node
/**
 * Compiles workouts/*.yaml into dist/workouts.json, the single file the watch app
 * fetches from GitHub Pages. Also writes a small dist/index.html so the Pages root
 * is readable by a human.
 *
 * Usage:
 *   node build-workouts.mjs           compile to dist/
 *   node build-workouts.mjs --check   validate only, write nothing
 */
import { readdirSync, readFileSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import Ajv from "ajv";
import { load } from "js-yaml";

const CATALOG_VERSION = 1;

const toolsDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(toolsDir, "..");
const sourceDir = join(repoRoot, "workouts");
const outputDir = join(repoRoot, "dist");
const checkOnly = process.argv.includes("--check");

const schema = JSON.parse(readFileSync(join(toolsDir, "workout.schema.json"), "utf8"));
const validate = new Ajv({ allErrors: true }).compile(schema);

const errors = [];
const workouts = [];
const seenIds = new Map();

const sourceFiles = readdirSync(sourceDir)
  .filter((file) => file.endsWith(".yaml") || file.endsWith(".yml"))
  .sort();

if (sourceFiles.length === 0) {
  console.error(`No workout files found in ${rel(sourceDir)}`);
  process.exit(1);
}

for (const file of sourceFiles) {
  const path = join(sourceDir, file);
  const slug = file.replace(/\.ya?ml$/, "");
  let document;

  try {
    document = load(readFileSync(path, "utf8"));
  } catch (error) {
    errors.push(`${rel(path)}: ${error.message.split("\n")[0]}`);
    continue;
  }

  if (document === null || typeof document !== "object" || Array.isArray(document)) {
    errors.push(`${rel(path)}: expected a YAML mapping with name and steps`);
    continue;
  }

  // The file name is the id unless the document sets one explicitly.
  const workout = { id: slug, ...document };

  if (!validate(workout)) {
    for (const error of validate.errors) {
      errors.push(`${rel(path)}: ${error.instancePath || "/"} ${error.message}`);
    }
    continue;
  }

  const duplicate = seenIds.get(workout.id);
  if (duplicate) {
    errors.push(`${rel(path)}: id "${workout.id}" is already used by ${duplicate}`);
    continue;
  }
  seenIds.set(workout.id, rel(path));

  workouts.push({
    id: workout.id,
    name: workout.name,
    ...(workout.description ? { description: workout.description } : {}),
    steps: workout.steps.map((step) => ({
      name: step.name,
      reps: step.reps,
      ...(step.notes ? { notes: step.notes } : {}),
    })),
  });
}

if (errors.length > 0) {
  console.error(`${errors.length} problem(s) found:\n`);
  for (const error of errors) console.error(`  ${error}`);
  console.error("\nSee tools/workout.schema.json for the expected shape.");
  process.exit(1);
}

const catalog = {
  version: CATALOG_VERSION,
  generatedAt: new Date().toISOString().replace(/\.\d{3}Z$/, "Z"),
  workouts,
};

const stepCount = workouts.reduce((total, workout) => total + workout.steps.length, 0);

if (checkOnly) {
  console.log(`OK: ${workouts.length} workout(s), ${stepCount} step(s).`);
  process.exit(0);
}

mkdirSync(outputDir, { recursive: true });
writeFileSync(join(outputDir, "workouts.json"), `${JSON.stringify(catalog, null, 2)}\n`);
writeFileSync(join(outputDir, "index.html"), renderIndex(catalog));

console.log(`Wrote ${rel(join(outputDir, "workouts.json"))}: ${workouts.length} workout(s), ${stepCount} step(s).`);

function rel(path) {
  return path.startsWith(repoRoot) ? path.slice(repoRoot.length + 1) : path;
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"]/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
  })[character]);
}

function renderIndex({ generatedAt, workouts }) {
  const sections = workouts
    .map(
      (workout) => `    <section>
      <h2>${escapeHtml(workout.name)} <code>${escapeHtml(workout.id)}</code></h2>
${workout.description ? `      <p>${escapeHtml(workout.description)}</p>\n` : ""}      <ol>
${workout.steps
  .map(
    (step) =>
      `        <li>${escapeHtml(step.name)} <b>&times;${step.reps}</b>${
        step.notes ? ` <i>${escapeHtml(step.notes)}</i>` : ""
      }</li>`,
  )
  .join("\n")}
      </ol>
    </section>`,
    )
    .join("\n");

  return `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Workouts</title>
    <style>
      :root { color-scheme: light dark; }
      body { font: 16px/1.5 system-ui, sans-serif; margin: 0 auto; max-width: 42rem; padding: 2rem 1rem; }
      h1 { margin-bottom: 0.25rem; }
      code { background: color-mix(in srgb, currentColor 12%, transparent); border-radius: 4px; padding: 0 0.3em; font-size: 0.85em; }
      section { margin-top: 2rem; }
      i { opacity: 0.7; }
      footer { margin-top: 3rem; opacity: 0.7; font-size: 0.85em; }
    </style>
  </head>
  <body>
    <h1>Workouts</h1>
    <p>Data for the Pixel Watch workout app: <a href="workouts.json">workouts.json</a></p>
${sections}
    <footer>Generated ${escapeHtml(generatedAt)} from the YAML files in <code>workouts/</code>.</footer>
  </body>
</html>
`;
}
