# BixisRewards

Daily, weekly, and monthly reward system plugin for YeditepeMC network.
Handles streak tracking, tiered mystery chest distribution via GadgetsMenu,
and Vault coin/XP rewards through BixisCore.

## Features
- Daily / weekly / monthly reward claiming
- Streak bonus system (3 gün: %25, 7 gün: %50 bonus)
- 5-tier mystery chest system (GadgetsMenu entegrasyonu)
- Guaranteed 1-2★ chest on daily claim (50/50)
- Guaranteed 3-4★ chest on weekly claim
- Guaranteed 5★ chest on monthly claim
- Turkish UI throughout

## Commands
| Komut | Açıklama |
|-------|----------|
| `/gunlukodul` | Günlük ödül menüsünü açar |
| `/günlüködül` | Alias |
| `/rewards admin give <oyuncu> <tier>` | Admin: oyuncuya kasa ver (şu anlık kullanım dışı)|

## Dependencies
- Paper 26.1.2
- Java 25
- BixisCore (required)
- Vault + EssentialsX (required, via BixisCore)
- GadgetsMenu (required, for chest distribution)

## Installation
1. Install BixisCore, Vault, EssentialsX, GadgetsMenu
2. Drop BixisRewards.jar into plugins/
3. Restart server