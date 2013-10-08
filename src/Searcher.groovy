import groovyx.gpars.GParsPool
import org.apache.commons.cli.Option

import java.nio.file.DirectoryStream
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths

def cli = new CliBuilder(usage:'Searches a pattern across multiple folders')
cli.with {
    p longOpt:'pattern', args:1, argName: 'pattern', 'Pattern to look for', required: true
    f longOpt:'folders', 'Folders to search', args: Option.UNLIMITED_VALUES, valueSeparator:',', required: true
    h longOpt: 'help', 'Help', args: 0, required: false
}
def options = cli.parse(args)

if (!options) {
    return
}

if (options.h) {
    cli.usage()
}

def show = {
    println it
}

def pattern = options.p
def folders = options.fs
def searchFolder = { closure, folder ->
    final Path dir = Paths.get(folder)
    if (!dir.toFile().exists()) {
        closure "Warning : ${folder} is not a correct folder, it won't be searched"
        return false
    }
    final java.nio.file.FileSystem fs = dir.getFileSystem()
    final PathMatcher matcher = fs.getPathMatcher("glob:*" + pattern + "*")
    final DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
        @Override
        public boolean accept(Path entry) throws IOException {
            return matcher.matches(entry.getFileName());
        }
    }
    final DirectoryStream<Path> paths = fs.provider().newDirectoryStream(dir, filter);
    final Iterator<Path> iterator = paths.iterator();
    iterator.each closure
    true
}
GParsPool.withPool {
    folders.eachParallel {
        def begin = new Date().getTime()
        def ok = searchFolder show, it
        def end = new Date().getTime()
        if (ok) {
            show "${it} -> ${end - begin} ms"
        }
    }
}
