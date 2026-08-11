@echo off

cd /d %~dp0

echo %cd%

echo コンパイル
javac -Xlint:unchecked -d .\classes .\src\main\java\com\ranorat\app\*.java
echo;

pause

echo java実行
cd .\classes
java com.ranorat.app.MainStudioClass
echo;

pause

echo jarファイル作成
jar cfm ..\jar\JStereoImageCropper.jar .\MANIFEST.MF .\com\ranorat\app\*.class .\resources
echo;

pause

echo jarファイル実行

java -jar ..\jar\JStereoImageCropper.jar
echo;

pause

