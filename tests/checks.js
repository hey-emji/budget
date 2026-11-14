// Checks run inside the app page (clock fixed to Nov 14, 2026, 10:00 Manila; data from fixture.json).
// Helpers: test(name, async fn), eq(a,b,msg), ok(v,msg), near(a,b,msg), wait(ms). Add a check for every bug fixed.
const tab=v=>document.querySelector(`nav .tab[data-v="${v}"]`).click();
const salId=()=>config.salaryTo||mainAcc().id;

await test("every tab and Settings render", async()=>{ for(const v of ["paydays","debts","settings","today"]){ tab(v); await wait(50); ok(!$("v-"+v).hidden,v+" hidden"); }
  openPrefs(); await wait(50); ok(!$("v-prefs").hidden,"settings hidden"); closePrefs(); await wait(50); });

await test("normal pay period is Oct 30 – Nov 14 and next payday is Nov 15", async()=>{ const p=periodNow(); eq(ymd(p.start),"2026-10-30"); eq(ymd(p.end),"2026-11-15"); eq(ymd(upcomingPaydays(1)[0]),"2026-11-15"); });

await test("Paydays tab opens on the next payday", async()=>{ pdIdx=0; tab("paydays"); await wait(50); ok($("v-paydays").querySelector(".pernav span").textContent.includes("Nov 15"),"title: "+$("v-paydays").querySelector(".pernav span").textContent); tab("today"); });

const balBefore=balances()[salId()].bal, nExp=expenses.length;
await test("Income → Salary records the payday's actual pay, adds no entry", async()=>{
  openSheet({mode:"inc"}); pickCat("inc-salary"); await wait(30); $("amt").value="24000"; $("dt").value="2026-11-13"; $("addForm").requestSubmit(); await wait(300);
  eq(expenses.length,nExp,"entries"); eq(paydays["2026-11-15"].income,24000); eq(paydays["2026-11-15"].got,"2026-11-13"); });
await test("early salary counts in the balance from the day it arrived", async()=>{ near(balances()[salId()].bal-balBefore,24000); });
await test("early salary starts the new period that day", async()=>{ const p=periodNow(); eq(ymd(p.start),"2026-11-13"); eq(ymd(p.end),"2026-11-30"); eq(ymd(periodAt(1).end),"2026-11-13"); });
await test("top card counts down to Nov 30 after early pay", async()=>{ eq(ymd(upcomingPaydays(1)[0]),"2026-11-30"); });
await test("Paydays tab still shows the received Nov 15 payday (ticks kept)", async()=>{ pdIdx=0; tab("paydays"); await wait(50); const t=$("v-paydays").querySelector(".pernav span").textContent; ok(t.includes("This payday")&&t.includes("Nov 15"),"title: "+t); tab("today"); });

await test("new loan counts in cards & loans for its first month", async()=>{ ok(instMonth("2026-11")>=15000,"instMonth "+instMonth("2026-11")); });

await test("Credit cards total doesn't double-count installments", async()=>{ tab("debts"); await wait(50); const B=balances();
  const regular=sum(CARDS().filter(c=>!c.lender),c=>Math.max(0,(B[c.id]||{}).owed||0)), oth=sum([...new Set(config.debts.map(d=>d.lender).filter(Boolean))],L=>otherOwed(L));
  eq($("sumCCAmt").textContent,peso2(regular+oth)); tab("today"); });

await test("auto-charged subscription is logged on its charge day", async()=>{
  await call("setConfig",{patch:{subs:[...config.subs,{name:"Testflix",amount:500,card:"maribank",day:13,from:"2026-11",fromDate:"2026-11-01"}]}}); await runAutoSubs();
  const e=expenses.filter(x=>x.autoSub&&x.autoSub.startsWith("testflix|")); eq(e.length,1); eq(e[0].date,"2026-11-13"); eq(e[0].acct,"maribank"); });
await test("bank message for that subscription updates the amount, no duplicate", async()=>{
  const n=await addToInbox([parseMessage("A MariCard Debit transaction of PHP 512.30 at TESTFLIX.COM was completed.")]); eq(n,0);
  const e=expenses.filter(x=>x.autoSub&&x.autoSub.startsWith("testflix|")); eq(e.length,1); eq(e[0].amount,512.3); eq(e[0].date,"2026-11-13"); });
await test("deleting an auto-charge doesn't bring it back", async()=>{ const e=expenses.find(x=>x.autoSub&&x.autoSub.startsWith("testflix|")); await call("delete",{id:e.id}); await runAutoSubs(); eq(expenses.filter(x=>x.autoSub&&x.autoSub.startsWith("testflix|")).length,0); });

const P=t=>parseMessage(t);
await test("reader: UnionBank transfer to self is a transfer", async()=>{ const r=P("You have successfully sent PHP30,000.00 to Test User via Instapay. Reference No.: UB123456."); eq(r.amount,30000); eq(readingToPre(r).mode,"xfer"); });
await test("reader: promos and OTPs are ignored", async()=>{ eq(P("Earn P2000 eGCs for a min. accum. spend of P100000. Register now, T&C apply."),null); eq(P("NEVER SHARE THIS OTP 123467 for CIGNAL purchase of PHP 10.00"),null); eq(P("658177 is your authentication code."),null); });
await test("reader: card purchase, e-wallet payment, withdrawal", async()=>{
  const a=P("Your MCC card 1111 was used at SM TICKET ECOM for PHP 9500.00. If you did not make this, call us."); eq(a.amount,9500); eq(a.type,"expense");
  const b=P("Your payment of P399.00 to Home Box Office has been successfully processed on 10-31-26 10:09:45 PM. Ref. No. 436976012"); eq(b.amount,399); eq(b.date,"2026-10-31");
  const c=P("Your Cardless Withdrawal transaction has been completed. Transaction Reference No. : 4353723624890 Transaction Date and Time : 11/11/2026 Amount Withdrawn : PHP 3,000.00"); eq(c.amount,3000); eq(readingToPre(c).mode,"xfer"); eq(readingToPre(c).to,"to-cash"); });
await test("reader: money received with no peso sign", async()=>{ const r=P("Money received. You received 12,660.00 in your wallet via InstaPay"); eq(r.type,"income"); eq(r.amount,12660); });
await test("reader: two notifications for the same payment become one", async()=>{ const before=INBOX().length;
  await addToInbox([P("Your payment of ₱1,790.00 to Maya Credit Cards on Nov 11, 2026 12:09 PM was successful."),P("You paid ₱1,790.00 to Maya Credit Cards on Nov 11, 2026 12:09 PM.")]); eq(INBOX().length-before,1); });

await test("To review → Review and save logs it and clears it", async()=>{ const n0=expenses.length, i0=INBOX().length; ok(i0>0,"inbox empty");
  tab("today"); await wait(50); document.querySelector("#inboxList .acts button").click(); await wait(50); $("addForm").requestSubmit(); await wait(300); eq(expenses.length,n0+1); eq(INBOX().length,i0-1); });

await test("back: closes the log panel, asks first if an amount is typed", async()=>{
  openSheet(); await wait(30); eq(window.__bujjetBack(),"handled"); ok($("addForm").hidden,"sheet should close");
  openSheet(); $("amt").value="99"; window.__bujjetBack(); await wait(30); ok(!$("dlg").hidden,"confirm should show"); $("dlgNo").click(); await wait(30); ok(!$("addForm").hidden,"sheet should stay"); closeSheet(); });
await test("back: other tab goes to Today, then warns before leaving", async()=>{ tab("debts"); await wait(30); window.__bujjetBack(); await wait(30); eq(curView,"today"); eq(window.__bujjetBack(),"handled"); eq(window.__bujjetBack(),"exit"); });
