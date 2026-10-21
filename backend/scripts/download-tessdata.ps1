# Télécharge les fichiers de langue Tesseract (français + anglais) dans backend/tessdata
$dest = Join-Path $PSScriptRoot "..\tessdata"
New-Item -ItemType Directory -Force $dest | Out-Null
foreach ($lang in "fra", "eng") {
    $url = "https://github.com/tesseract-ocr/tessdata_fast/raw/main/$lang.traineddata"
    Write-Host "Téléchargement de $lang.traineddata..."
    Invoke-WebRequest -Uri $url -OutFile (Join-Path $dest "$lang.traineddata")
}
Write-Host "OK : fichiers enregistrés dans $dest"
