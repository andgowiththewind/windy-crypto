@echo off
echo ----------------------------------[zip][start]---------------------------------------------------------------------
echo "processing compressed packaging..."
call :getDatetimeFormat
set zipFileName=windy_crypto_%return%.zip
echo "zipFileName: %zipFileName%"
"C:/cafe/app/zip/WinRAR.exe" a -r %zipFileName% ./windy-crypto.sqlite
echo done.
echo ----------------------------------[zip][end  ]---------------------------------------------------------------------
pause



:getDatetimeFormat
for /f "tokens=2 delims==" %%a in ('wmic os get localdatetime /value') do set datetime=%%a
set "year=%datetime:~0,4%"
set "month=%datetime:~4,2%"
set "day=%datetime:~6,2%"
set "hour=%datetime:~8,2%"
set "min=%datetime:~10,2%"
set "sec=%datetime:~12,2%"
if "%month:~0,1%"==" " set "month=0%month:~1%"
if "%day:~0,1%"==" " set "day=0%day:~1%"
if "%hour:~0,1%"==" " set "hour=0%hour:~1%"
if "%min:~0,1%"==" " set "min=0%min:~1%"
if "%sec:~0,1%"==" " set "sec=0%sec:~1%"
set datetimeStr=%year%%month%%day%_%hour%%min%%sec%
set return=%datetimeStr%
goto:eof