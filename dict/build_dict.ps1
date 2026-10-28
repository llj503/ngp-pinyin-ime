$ErrorActionPreference = 'Stop'
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$cedict = Join-Path $dir 'cedict.txt'
$jieba = Join-Path $dir 'jieba-dict.txt'
$out = Join-Path $dir 'dict.tsv'

$freq = New-Object 'System.Collections.Generic.Dictionary[string,int]'
foreach ($line in [IO.File]::ReadLines($jieba)) {
    if ($line.Length -eq 0) { continue }
    $p = $line -split '\s+', 3
    if ($p.Count -lt 2) { continue }
    $n = 0
    if ([int]::TryParse($p[1], [ref]$n) -and $n -gt 0) {
        $freq[$p[0]] = $n
    }
}
Write-Host "jieba words: $($freq.Count)"

$seen = New-Object 'System.Collections.Generic.Dictionary[string,int]'
$re = [regex]'^(\S+)\s+(\S+)\s+\[([^\]]+)\]\s+/(.*)/\s*$'
$total = 0
$kept = 0
foreach ($line in [IO.File]::ReadLines($cedict)) {
    if ($line.Length -eq 0 -or $line[0] -eq '#') { continue }
    $total++
    $m = $re.Match($line)
    if (-not $m.Success) { continue }
    $word = $m.Groups[2].Value
    if ($word -notmatch '^[\u4e00-\u9fff]+$') { continue }
    $py = $m.Groups[3].Value.ToLowerInvariant()
    $py = $py -replace 'u:', 'v' -replace 'ü', 'v'
    $py = $py -replace '[^a-zv]', ''
    if ($py.Length -eq 0 -or $py.Length -gt 40) { continue }
    $f = 0
    if ($freq.ContainsKey($word)) { $f = $freq[$word] }
    if ($f -le 0) { $f = 1 }
    $k = $py + '|' + $word
    if (-not $seen.ContainsKey($k) -or $seen[$k] -lt $f) {
        $seen[$k] = $f
    }
    $kept++
}
Write-Host "cedict lines: $total, kept entries: $kept, unique: $($seen.Count)"

$sb = New-Object System.Text.StringBuilder
foreach ($kv in $seen.GetEnumerator()) {
    $parts = $kv.Key.Split('|')
    [void]$sb.Append($parts[0]).Append("`t").Append($parts[1]).Append("`t").Append($kv.Value).Append("`n")
}
[IO.File]::WriteAllText($out, $sb.ToString(), (New-Object System.Text.UTF8Encoding($false)))
Write-Host "wrote $out ($((Get-Item $out).Length) bytes)"

$gz = Join-Path (Split-Path $dir -Parent) 'app\assets\dict.tsv.gz'
$src = [IO.File]::OpenRead($out)
$dst = [IO.File]::Create($gz)
$gzs = New-Object IO.Compression.GZipStream($dst, [IO.Compression.CompressionLevel]::Optimal)
$src.CopyTo($gzs)
$gzs.Dispose(); $dst.Dispose(); $src.Dispose()
Write-Host "wrote $gz"
