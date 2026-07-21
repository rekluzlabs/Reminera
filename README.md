# Reminera

<p align="center">  
  <img width="550" alt="Reminera" src="https://raw.githubusercontent.com/rekluzlabs/Reminera/main/1784605706900-removebg-preview.png" />
</p>


Preserve your family's history, stories, and voices — completely offline and under your control.

Reminera is an offline-first Android application built to document, organize, and safeguard the memories, personal histories, and voices of family members — especially older relatives — before their stories are lost to time.

---

## 🌟 Key Features

* Offline-First & Private: All personal data (narratives, family profiles, historical photographs, audio clips, and video recordings) stays strictly local on your device. No required accounts, no tracking, and no external server dependency.
* Rich Storytelling Canvas: Attach multiple mixed-media items to any single memory:
  - 📷 Historical Photos: Import digitized family photographs via the native Android Photo Picker.
  - 🎙️ Audio Recordings: Capture voice stories, oral histories, and interviews directly in-app.
  - 📹 Video Clips: Record video memories using CameraX.
  - 📝 Narratives & Captions: Write detailed story accounts and add individual captions and ordering to media assets.
* Family Profiles: Organize memories by relative, keeping timelines, biographies, key dates, and relationships clear.
* Print-Ready Book Export (Future Phase): Layout stories, photos, and embedded QR codes into print-ready PDF memory books. Scan QR codes on printed pages to instantly play linked audio/video clips hosted in your personal cloud storage (BYOK-style).

---

## 🏗️ Architecture & Technical Stack

* Language: Kotlin
* UI Framework: Jetpack Compose
* Architecture: MVVM + Clean Architecture
* Database: Room DB with @TypeConverter support and custom relational wrappers (MemoryWithMedia)
* Camera & Capture: CameraX & MediaRecorder
* Export Engine (Phase 2): Native Android PdfDocument, Canvas, and ZXing QR code generation

---

## 📂 Data Model Overview

* FamilyMemberEntity: Stores relative profiles, relationships, birth dates, avatars, and biographies.
* MemoryEntity: Represents a specific memory or story event linked to a family member.
* MediaItemEntity: Individual media assets linked to a memory, supporting type-scoped displayOrder, caption, and optional remoteShareUrl for future book export.

---

## 🗺️ Development Roadmap

### Phase 1: Local Memory Preservation (Current Focus)
- [x] Room Database architecture setup & entity schema design
- [ ] Family member profile creation and management
- [ ] Story editor with rich text entry
- [ ] CameraX audio/video capture integration
- [ ] System Photo Picker import integration
- [ ] Timeline & memory filter views

### Phase 2: Physical Book & QR Linkage (Future)
- [ ] Dynamic high-DPI (300 DPI) PDF layout engine with pagination
- [ ] BYOK cloud storage integration (Google Drive / Dropbox)
- [ ] ZXing QR matrix generation for linked cloud media
- [ ] Print preview and PDF export launcher

---

## 🔒 Privacy Philosophy

Reminera adheres to a strict "Your Data, Your Ownership" philosophy. All assets remain entirely on your local file system unless you explicitly choose to link cloud storage for physical book exports.

---

## 🛠️ Build & Setup

1. Clone the repository: git clone https://github.com/rekluzlabs/Reminera.git
2. Open the project in your IDE.
3. Sync Gradle and build the project.

---

## 📄 License

Created and maintained by RekluzLabs.
