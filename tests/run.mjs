// bujjet test runner: loads index.html in headless Chrome with a fixed clock and synthetic data,
// runs the checks in checks.js, and fails if any check fails. Usage: node tests/run.mjs
import { readFileSync, writeFileSync, mkdtempSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join, dirname } from "node:path";
import { tmpdir } from "node:os";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const html = readFileSync(join(root, "index.html"), "utf8");
const fixture = readFileSync(join(root, "tests/fixture.json"), "utf8");
const checks = readFileSync(join(root, "tests/checks.js"), "utf8");
const NOW = process.env.BUJJET_NOW || "2026-11-14T10:00:00+08:00";

const clock = `<script>(()=>{const F=new Date(${JSON.stringify(NOW)}).getTime(),R=Date;class D extends R{constructor(...a){a.length?super(...a):super(F)}static now(){return F}};window.Date=D;})();</script>`;
const a = html.indexOf("async function idbGet"), b = html.indexOf("\n", html.indexOf("async function idbSet"));
if (a < 0 || b < 0 || !html.includes("load();\n\n</script>")) { console.error("Test hooks not found in index.html"); process.exit(2); }
let page = html.slice(0, a) + `window.__mem={state:${fixture}};async function idbGet(k){return window.__mem[k]??null}\nasync function idbSet(k,v){window.__mem[k]=v}` + html.slice(b);
page = page.replace("<head>", "<head>" + clock).replace("</head>", "<style>*{animation:none!important;transition:none!important}</style></head>");
page = page.replace("load();\n\n</script>", `window.__errors=[];window.addEventListener("error",e=>window.__errors.push(String(e.message)));
load();
setTimeout(async()=>{const R=[];const wait=ms=>new Promise(r=>setTimeout(r,ms));
  const test=async(name,fn)=>{try{await fn();R.push([name,"ok"])}catch(e){R.push([name,"FAIL: "+(e&&e.message||e)])}};
  const eq=(a,b,msg)=>{if(JSON.stringify(a)!==JSON.stringify(b))throw new Error((msg||"")+" expected "+JSON.stringify(b)+" got "+JSON.stringify(a))};
  const ok=(v,msg)=>{if(!v)throw new Error(msg||"not true")};
  const near=(a,b,msg)=>{if(Math.abs(a-b)>0.01)throw new Error((msg||"")+" expected ~"+b+" got "+a)};
  try{
${checks}
  }catch(e){R.push(["runner","FAIL: "+e.message])}
  if(window.__errors.length) R.push(["no page errors","FAIL: "+window.__errors.join(" | ")]); else R.push(["no page errors","ok"]);
  document.body.dataset.result=JSON.stringify(R);},1200);
</script>`);

const dir = mkdtempSync(join(tmpdir(), "bujjet-test-"));
const file = join(dir, "page.html");
writeFileSync(file, page);
const chrome = process.env.CHROME || ["google-chrome", "chromium", "chromium-browser"].find(c => { try { execFileSync("which", [c]); return true } catch { return false } });
const out = execFileSync(chrome, ["--headless=new", "--no-sandbox", "--disable-gpu", "--virtual-time-budget=30000", "--dump-dom", "file://" + file],
  { env: { ...process.env, TZ: "Asia/Manila" }, maxBuffer: 50e6, stdio: ["ignore", "pipe", "ignore"] }).toString();
const m = out.match(/data-result="([^"]*)"/);
if (!m) { console.error("No results. The page may have crashed before the checks ran."); process.exit(1); }
const res = JSON.parse(m[1].replace(/&quot;/g, '"').replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&#39;/g, "'"));
let fail = 0;
for (const [n, r] of res) { console.log((r === "ok" ? "  ✓ " : "  ✗ ") + n + (r === "ok" ? "" : "  — " + r)); if (r !== "ok") fail++; }
console.log(`\n${res.length - fail} passed, ${fail} failed`);
process.exit(fail ? 1 : 0);
