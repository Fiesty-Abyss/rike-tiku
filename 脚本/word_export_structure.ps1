param(
    [Parameter(Mandatory = $true)]
    [string]$SourceDoc,

    [Parameter(Mandatory = $true)]
    [string]$ConvertedDocx,

    [Parameter(Mandatory = $true)]
    [string]$VisualPdf
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
$document = $null

try {
    $document = $word.Documents.Open($SourceDoc, $false, $true)

    # wdFormatXMLDocument = 12. 这只是临时结构副本，不改写原始 .doc。
    $document.SaveAs2($ConvertedDocx, 12)
    if (-not (Test-Path -LiteralPath $ConvertedDocx)) {
        throw "Word未落盘临时DOCX结构副本: $ConvertedDocx"
    }

    # wdExportFormatPDF = 17。PDF 仅用于视觉核验，不参与题目解析。
    $document.ExportAsFixedFormat($VisualPdf, 17)
    if (-not (Test-Path -LiteralPath $VisualPdf)) {
        throw "Word未落盘视觉核验PDF: $VisualPdf"
    }

    $objects = @()
    for ($index = 1; $index -le $document.InlineShapes.Count; $index++) {
        $shape = $document.InlineShapes.Item($index)
        $progId = $null
        try {
            $progId = $shape.OLEFormat.ProgID
        }
        catch {
            $progId = $null
        }
        $objects += [PSCustomObject]@{
            kind = "inline"
            index = $index
            start = $shape.Range.Start
            page = $shape.Range.Information(3)
            type = $shape.Type
            progId = $progId
            width = [math]::Round($shape.Width, 2)
            height = [math]::Round($shape.Height, 2)
        }
    }

    for ($index = 1; $index -le $document.Shapes.Count; $index++) {
        $shape = $document.Shapes.Item($index)
        $objects += [PSCustomObject]@{
            kind = "shape"
            index = $index
            start = $shape.Anchor.Start
            page = $shape.Anchor.Information(3)
            type = $shape.Type
            progId = $null
            width = [math]::Round($shape.Width, 2)
            height = [math]::Round($shape.Height, 2)
        }
    }

    $objects = @($objects | Sort-Object start, kind, index)
    $result = [PSCustomObject]@{
        paragraphs = $document.Paragraphs.Count
        tables = $document.Tables.Count
        inlineShapes = $document.InlineShapes.Count
        shapes = $document.Shapes.Count
        omaths = $document.OMaths.Count
        pages = $document.ComputeStatistics(2)
        sections = $document.Sections.Count
        words = $document.ComputeStatistics(0)
        characters = $document.ComputeStatistics(3)
        objects = $objects
    }
    $result | ConvertTo-Json -Depth 6 -Compress
}
finally {
    if ($null -ne $document) {
        $document.Close(0)
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($document)
    }
    $word.Quit()
    [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word)
}
