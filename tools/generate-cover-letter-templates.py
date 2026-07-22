#!/usr/bin/env python3
"""Derive CoverLetterTemplate.kt from the iOS source of truth.

    python3 tools/generate-cover-letter-templates.py <path-to-CoverLetterDocument.swift>
"""
import re
import sys

src = open(sys.argv[1]).read()
enum_start = src.index('enum CoverLetterTemplate')
cases = re.findall(r'^  case ([a-z][a-zA-Z]*)$',
                   src[enum_start:src.index('var id: String { rawValue }', enum_start)], re.M)
titles = dict(re.findall(r'case \.(\w+): "([^"]+)"',
                         src[src.index('var title: String {'):src.index('var subtitle:')]))
pairs = dict(re.findall(r'case \.(\w+): \.(\w+)',
                        src[src.index('var pairsWith: ResumeTemplate?'):src.index('var title: String {')]))

assert set(cases) == set(titles), "cases and titles disagree"
print(f"{len(cases)} cover-letter templates, {len(pairs)} paired with a résumé template")
for c in cases:
    print(f"  {c:12} {titles[c]:26} -> {pairs.get(c, '-')}")
