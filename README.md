# Multithreading

Программа проходит по дереву каталогов и счтает hash(SHA256) у свсех файлов.
Резкльтаты записывает в файл.

Для работы берет параметры из файла config.prop
Параметры:
distribspath - корневой каталог, где начать поиск;
outfile - имя файла, для записи результатов;
threadcount - количество параллельных потоков выполнения;
