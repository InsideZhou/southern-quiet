package me.insidezhou.southernquiet.filesystem;

import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static me.insidezhou.southernquiet.filesystem.FileSystem.PATH_SEPARATOR_STRING;


/**
 * 规格化的路径。
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class NormalizedPath implements Serializable {
    private final static long serialVersionUID = -8700923055584230554L;

    public final static NormalizedPath ROOT = new NormalizedPath(PATH_SEPARATOR_STRING);

    public NormalizedPath(String path) {
        init(path);
    }

    public NormalizedPath(String[] pathElements) {
        if (0 == pathElements.length) {
            init(PATH_SEPARATOR_STRING);
            return;
        }

        init(String.join(PATH_SEPARATOR_STRING, pathElements));
    }

    private void init(String path) {
        path = path.replace('\\', '/');

        List<String> pathList = Arrays.stream(path.split(PATH_SEPARATOR_STRING))
            .filter(p -> !StringUtils.isEmpty(p))
            .collect(Collectors.toList());

        if (0 == pathList.size()) {
            name = PATH_SEPARATOR_STRING;
        }
        else if (1 == pathList.size()) {
            this.setParentNames(new String[]{PATH_SEPARATOR_STRING});
            name = pathList.get(0);
        }
        else {
            pathList.add(0, PATH_SEPARATOR_STRING);

            int lastIndex = pathList.size() - 1;
            this.setName(pathList.get(lastIndex));
            this.setParentNames(pathList.subList(0, lastIndex).toArray(new String[0]));
        }
    }

    /**
     * 路径父级的名称集合。当前路径是根路径时，为空集合，否则第一个元素为{@link FileSystem#PATH_SEPARATOR_STRING}。
     */
    private String[] parentNames = new String[0];
    /**
     * 路径名称。当前路径是根路径时，为{@link FileSystem#PATH_SEPARATOR_STRING}。
     */
    private String name = "";

    public String[] getParentNames() {
        return parentNames;
    }

    public void setParentNames(String[] parentNames) {
        this.parentNames = parentNames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        switch (parentNames.length) {
            case 0:
                return "";
            case 1:
                return parentNames[0];
            default:
                return String.join(PATH_SEPARATOR_STRING, parentNames).substring(1);
        }
    }

    public NormalizedPath getParentPath() {
        return new NormalizedPath(getParentNames());
    }

    /**
     * 规格化过的路径名，以 {@link FileSystem#PATH_SEPARATOR} 开头且不以其结尾。
     *
     * @return 根路径会返回 {@link FileSystem#PATH_SEPARATOR_STRING}
     */
    @Override
    public String toString() {
        switch (parentNames.length) {
            case 0:
                return name;
            case 1:
                return PATH_SEPARATOR_STRING + name;
            default:
                return String.join(PATH_SEPARATOR_STRING, parentNames).substring(1) + PATH_SEPARATOR_STRING + name;
        }
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NormalizedPath)) return false;

        NormalizedPath that = (NormalizedPath) o;

        return this.toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
