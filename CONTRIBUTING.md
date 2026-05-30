# Contributing to EdgeFlow

Thank you for your interest in contributing to EdgeFlow! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Enhancements](#suggesting-enhancements)
- [Pull Requests](#pull-requests)
- [Development Setup](#development-setup)
- [Coding Guidelines](#coding-guidelines)

---

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md) (if applicable). Please be respectful and considerate towards others.

---

## How Can I Contribute?

There are many ways to contribute to EdgeFlow:

- 🐛 **Report bugs** — help us identify and fix issues
- 💡 **Suggest features** — share ideas to make EdgeFlow better
- 📖 **Improve documentation** — fix typos, clarify wording, add examples
- 🌍 **Add translations** — help localize EdgeFlow into more languages
- 💻 **Submit pull requests** — fix bugs, implement features, refactor code

---

## Reporting Bugs

Before creating a bug report, please check [existing issues](https://github.com/btm-m/EdgeFlow/issues) to avoid duplicates.

### When opening a new issue, please include:

- **Device info**: Android version, device model, ROM name (if custom ROM)
- **App version**: EdgeFlow version name and version code
- **Shizuku status**: whether Shizuku is installed/paired
- **Steps to reproduce**: clear, step-by-step instructions
- **Expected behavior** vs **actual behavior**
- **Screenshots/logs**: if applicable

### Issue Template

```
**Device**: [e.g. Pixel 7, Android 14]
**App Version**: [e.g. 1.0.1]
**Shizuku**: [Installed / Not installed]

**Steps to Reproduce**:
1. ...
2. ...

**Expected**: ...

**Actual**: ...
```

---

## Suggesting Enhancements

Enhancement suggestions are welcome! Please open an issue with:

- A clear **title** and **description** of the proposed feature
- **Why** this feature would be useful
- **Mockups/screenshots** if the feature involves UI changes
- **Alternatives considered** (if any)

---

## Pull Requests

### Branch Naming

| Type | Prefix | Example |
|---|---|---|
| Feature | `feat/` | `feat/custom-gesture` |
| Bugfix | `fix/` | `fix/sidebar-crash` |
| Docs | `docs/` | `docs/update-readme` |
| Refactor | `refactor/` | `refactor/room-migration` |

### Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add custom gesture sensitivity setting
fix: resolve crash when Shizuku is not paired
docs: update CONTRIBUTING.md
refactor: migrate from Views to Compose for About screen
```

### PR Checklist

Before submitting a PR, please make sure:

- [ ] The code **builds successfully** with `./gradlew assembleDebug`
- [ ] **Kotlin style guidelines** are followed (see [Coding Guidelines](#coding-guidelines))
- [ ] New features include **appropriate comments** where necessary
- [ ] **Proguard rules** are updated if new libraries or reflection is used
- [ ] **Screenshots** are added for UI changes
- [ ] The PR description clearly describes the problem and solution

### PR Title Format

```
feat: add XX feature
fix: fix XX issue
docs: update XX
refactor: refactor XX
```

---

## Development Setup

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 17 (recommended) or 11+
- **Android SDK** Platform 35
- **Kotlin** plugin (bundled with Android Studio)
- (Optional) **Shizuku** installed on test device

### Steps

```bash
# 1. Fork the repo on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/EdgeFlow.git
cd EdgeFlow

# 2. Add upstream remote
git remote add upstream https://github.com/btm-m/EdgeFlow.git

# 3. Create a feature branch
git checkout -b feat/your-feature-name

# 4. Make your changes, then build and test
./gradlew assembleDebug

# 5. Commit and push
git add .
git commit -m "feat: add your feature"
git push origin feat/your-feature-name

# 6. Open a Pull Request on GitHub
```

---

## Coding Guidelines

### Kotlin Style

- Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **meaningful variable/function names**
- Keep functions **short and focused** (single responsibility)
- Use **data classes** for models
- Prefer **immutability** (`val` over `var`) where possible

### Compose Guidelines

- Use **Material Design 3** components and color system
- Keep composables **stateless** when possible (hoist state)
- Use **`remember` / `rememberSaveable`** appropriately
- Follow the [Compose Mentions](https://developer.android.com/jetpack/compose/mentions) best practices

### Architecture

EdgeFlow follows **Clean Architecture** principles:

```
app/src/main/java/btm/m/edgeflow/
├── ui/           # Compose UI layer (screens, components)
├── viewmodel/     # State holders (ViewModel)
├── data/         # Repository, Room database, DataStore
├── di/           # Hilt dependency injection modules
├── service/      # Android services (Shizuku, MediaListener)
├── receiver/     # Broadcast receivers
└── util/        # Utility/extension functions
```

### String Resources

- All user-facing strings **must** go into `res/values/strings.xml`
- **Do NOT** hardcode strings in Kotlin/Compose code
- Add translated strings to `res/values-zh/strings.xml` (Chinese)

### Proguard

If your change adds new libraries or uses reflection, update `app/proguard-rules.pro` accordingly.

---

## License Agreement

By contributing, you agree that your contributions will be licensed under the [GNU General Public License v3.0](LICENSE).

---

## Questions?

Feel free to open a [Discussion](https://github.com/btm-m/EdgeFlow/discussions) or reach out via [btm-m's Blog](https://btm-m.live).

Thank you for contributing to EdgeFlow! 🎉
