#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const isVercel = !!process.env.VERCEL;
console.log('--- CivicShield Build Orchestrator ---');
console.log(`Environment: ${isVercel ? 'Vercel Cloud' : 'Standard / Local'}`);

// 1. Ensure public directory exists
const publicDir = path.join(__dirname, '..', 'public');
if (!fs.existsSync(publicDir)) {
  fs.mkdirSync(publicDir, { recursive: true });
}

// 2. Copy static resources into public/
const staticSource = path.join(__dirname, '..', 'src', 'main', 'resources', 'static');
if (fs.existsSync(staticSource)) {
  copyDir(staticSource, publicDir);
}

// 3. Check for Maven availability
const javaHome = process.env.JAVA_HOME || '/opt/homebrew/opt/openjdk@17';
const env = { ...process.env, JAVA_HOME: javaHome, PATH: `${javaHome}/bin:${process.env.PATH}` };

try {
  execSync('mvn -v', { stdio: 'ignore', env });
  hasMaven = true;
} catch (e) {
  hasMaven = false;
}

if (isVercel || !hasMaven) {
  console.log('⚡ Vercel / Cloud CI detected without Maven.');
  console.log('📦 Static web distribution prepared in ./public');
  console.log('ℹ️  CivicShield Spring Boot Java server deploys via Docker / Render (see render.yaml).');
  console.log('✅ Vercel build completed successfully!');
  process.exit(0);
} else {
  console.log('☕ Maven found. Building Spring Boot 3 Java application...');
  execSync('mvn clean package -DskipTests', { stdio: 'inherit', env });
  console.log('✅ Spring Boot package built successfully!');
}

function copyDir(src, dest) {
  if (!fs.existsSync(dest)) fs.mkdirSync(dest, { recursive: true });
  const entries = fs.readdirSync(src, { withFileTypes: true });
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyDir(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}
