#!/usr/bin/env python3
"""Derive ResumeAccent.kt from the iOS source of truth.

The colours drive the exported PDF, so they are lifted from `ResumeDocument.swift`
rather than transcribed. Usage:

    python3 tools/generate-accents.py <path-to-ResumeDocument.swift>
"""
import re
import sys

swift = open(sys.argv[1]).read()
enum_start = swift.index('enum ResumeAccent')

names = dict(re.findall(r'case \.(\w+): "([^"]+)"',
                        swift[swift.index('var title: LocalizedStringResource'):swift.index('var isPremium')]))
colors = re.findall(
    r'case \.(\w+):\s*\n\s*UIColor\(red: ([\d.]+), green: ([\d.]+), blue: ([\d.]+), alpha: 1\)',
    swift[swift.index('var uiColor'):])
order = re.findall(r'^  case (\w+)$',
                   swift[enum_start:swift.index('var id: String { rawValue }', enum_start)], re.M)

cmap = {n: (r, g, b) for n, r, g, b in colors}
tiers = {**{n: 'SIGNATURE' for n in ['emerald', 'amethyst', 'sapphire', 'rose', 'midnight']},
         **{n: 'ATELIER' for n in ['bronze', 'graphite', 'plum', 'steel', 'terracotta']},
         **{n: 'LUXE' for n in ['champagne', 'peacock', 'mulberry', 'moss', 'cobalt', 'cocoa']}}

assert set(order) == set(cmap) == set(names), "accent cases, colours and titles disagree"
print(f"{len(order)} accents parsed — regenerate ResumeAccent.kt from these values")
for n in order:
    print(f"  {n:12} {names[n]:16} {cmap[n]}  {tiers.get(n, 'INCLUDED')}")
