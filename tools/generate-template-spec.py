#!/usr/bin/env python3
"""Derive the shared template-plan spec from the iOS source of truth.

Reads `ResumeDocument.swift`, resolves every ResumeTemplate case to a fully
expanded TemplatePlan, and writes the JSON both platforms are tested against.
"""
import json
import re
import sys

SWIFT = sys.argv[1]
OUT_JSON = sys.argv[2]

src = open(SWIFT).read().splitlines()

# --- 1. the enum cases -------------------------------------------------------
start = next(i for i, l in enumerate(src) if l.startswith("enum ResumeTemplate"))
templates = []
for line in src[start + 1:]:
    m = re.match(r"^  case ([a-z][a-zA-Z]*)$", line)
    if m:
        templates.append(m.group(1))
    elif re.match(r"^  (var|func|static)", line):
        break

# --- 2. the plan switch ------------------------------------------------------
pstart = next(i for i, l in enumerate(src) if "var plan: TemplatePlan" in l)
pend = next(i for i, l in enumerate(src[pstart + 1:], pstart + 1) if l == "  }")
body = "\n".join(src[pstart:pend])

DEFAULT = {
    "body": {"kind": "single"},
    "experience": "stacked",
    "competencies": "bullets",
    "sectionChrome": "plain",
    "contact": "inline",
    "education": None,
    "references": None,
    "additional": None,
    "skillsFirst": False,
    "numberedSections": False,
    "darkPaper": False,
    "hangingHeadings": False,
    "profileInHeader": False,
    "bodyInset": 0.0,
    "density": 1.0,
}


def split_args(s):
    """Split a Swift argument list on top-level commas."""
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch in "([":
            depth += 1
        elif ch in ")]":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def balanced(expr, opener):
    """Text inside the parens that follow `opener`, respecting nesting."""
    i = expr.index(opener) + len(opener)
    depth, out = 1, ""
    while depth:
        ch = expr[i]
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                break
        out += ch
        i += 1
    return out


def parse_side(expr):
    """`.side(SideColumn(edge: .leading, width: 186, fill: .dark, ...))`"""
    inner = balanced(expr, "SideColumn(")
    col = {"edge": None, "width": None, "fill": None,
           "startsBelowProfile": False, "divider": False}
    for arg in split_args(inner):
        k, v = arg.split(":", 1)
        k, v = k.strip(), v.strip()
        if k in ("edge", "fill"):
            col[k] = v.lstrip(".")
        elif k == "width":
            col[k] = float(v)
        else:
            col[k] = v == "true"
    return {"kind": "side", "column": col}


def parse_plan(expr):
    plan = json.loads(json.dumps(DEFAULT))
    inner = balanced(expr, "TemplatePlan(")
    if not inner.strip():
        return plan
    for arg in split_args(inner):
        k, v = arg.split(":", 1)
        k, v = k.strip(), v.strip()
        if k == "body":
            plan["body"] = parse_side(v) if v.startswith(".side") else {"kind": "single"}
        elif k in ("bodyInset", "density"):
            plan[k] = float(v)
        elif v in ("true", "false"):
            plan[k] = v == "true"
        else:
            plan[k] = v.lstrip(".")
    return plan


# Walk the switch: `case .a, .b:` then everything up to the next case/default.
blocks = re.split(r"\n    (?=case |default:)", body)
resolved = {}
for blk in blocks:
    if not blk.strip().startswith("case "):
        continue
    head, _, tail = blk.partition(":")
    names = [n.strip().lstrip(".") for n in head.strip()[len("case "):].split(",")]
    names = [n for n in names if re.fullmatch(r"[a-z][a-zA-Z]*", n)]
    expr = " ".join(l.strip() for l in tail.strip().splitlines())
    if not expr.startswith("TemplatePlan"):
        continue
    plan = parse_plan(expr)
    for n in names:
        resolved[n] = plan

catalogue = {t: resolved.get(t, DEFAULT) for t in templates}

json.dump(
    {
        "$comment": "GENERATED from iOS ResumeDocument.swift by tools/generate-template-spec.py. Do not hand-edit.",
        "schemaVersion": 1,
        "templateCount": len(templates),
        "templates": catalogue,
    },
    open(OUT_JSON, "w"),
    indent=2,
    sort_keys=False,
)

nondefault = sum(1 for t in templates if resolved.get(t) not in (None, DEFAULT))
print(f"templates: {len(templates)}   with a non-default plan: {nondefault}")
print(f"unmatched switch names: {sorted(set(resolved) - set(templates))}")
