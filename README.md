<p align="center"> <img width="550" alt="Reminera" src="https://raw.githubusercontent.com/rekluzlabs/Reminera/main/1784605706900-removebg-preview.png" /> </p> <h1 align="center">Reminera</h1>
Preserve your family's history, stories, and voices — completely offline and under your control.

Reminera is an offline-first Android application being built to document, organize, and safeguard the memories, personal histories, and voices of family members — especially older relatives — before their stories are lost to time.

Status: Very Early Alpha. Reminera is in very early development. Most features described below are planned and not yet implemented. This README describes the intended direction of the project, not its current state. See the Development Roadmap for what actually exists today.

Vision & Planned Features
Offline-First & Private: All personal data (narratives, family profiles, historical photographs, audio clips, and video recordings) is intended to stay strictly local on your device. 
No required accounts, no tracking, and no external server dependency although online storage will be an backup option if desired.
Rich Storytelling Canvas (Planned): Attach multiple mixed-media items to any single memory:
Historical Photos: Import digitized family photographs via the native Android Photo Picker.
Audio Recordings: Capture voice stories, oral histories, and interviews directly in-app.
Video Clips: Record video memories using CameraX.
Narratives & Captions: Write detailed story accounts and add individual captions and ordering to media assets.
Family Profiles (Planned): Organize memories by relative, keeping timelines, biographies, key dates, and relationships clear.
Print-Ready Book Export (Future Phase): Layout stories, photos, and embedded QR codes into print-ready PDF memory books. Scan QR codes on printed pages to instantly play linked audio/video clips hosted in your personal cloud storage.

None of the features above are guaranteed to ship in their current described form — this is a solo indie project and the design may change as it's built.

Architecture & Technical Stack (Target)

The following reflects the intended architecture as the app is built out. Not all of it is implemented yet.

Language: Kotlin
UI Framework: Jetpack Compose
Architecture: MVVM + Clean Architecture
Database: Room DB with @TypeConverter support and custom relational wrappers (MemoryWithMedia)
Camera & Capture: CameraX & MediaRecorder
Export Engine (Phase 2): Native Android PdfDocument, Canvas, and ZXing QR code generation
Data Model Overview (In Progress)

Initial schema design has started; entities are subject to change as features are implemented.

FamilyMemberEntity: Stores relative profiles, relationships, birth dates, avatars, and biographies.
MemoryEntity: Represents a specific memory or story event linked to a family member.
MediaItemEntity: Individual media assets linked to a memory, supporting type-scoped displayOrder, caption, and optional remoteShareUrl for future book export.
Development Roadmap
Phase 1: Local Memory Preservation (Current Focus)
 Initial rough alpha build released
 Room Database architecture setup & entity schema design
 Family member profile creation and management
 Story editor with rich text entry
 CameraX audio/video capture integration
 System Photo Picker import integration
 Timeline & memory filter views
Phase 2: Physical Book & QR Linkage (Future)
 Dynamic high-DPI (300 DPI) PDF layout engine with pagination
 BYOK cloud storage integration (Google Drive / Dropbox)
 ZXing QR matrix generation for linked cloud media
 Print preview and PDF export launcher
Privacy Philosophy

Reminera adheres to a strict "Your Data, Your Ownership" philosophy. All assets are intended to remain entirely on your local file system, with no external server dependency. This section will be updated to reflect actual implemented behavior as each feature ships.

License

Copyright (c) 2026 Rekluz Labs. All rights reserved.

This software and all associated files (the "Software") are the exclusive intellectual property of Rekluz Labs.

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION:

PROPRIETARY RIGHTS: This Software is not open-source. All rights, title, and interest in and to the Software remain with Rekluz Labs.
PERSONAL USE ONLY: You are granted permission to download and view the source code for educational and personal purposes only.
RESTRICTIONS:
You may not redistribute, sell, or sub-license the Software.
You may not create derivative works for commercial gain.
You may not use the branding, logo, or assets for any purpose without express written consent.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER LIABILITY.
