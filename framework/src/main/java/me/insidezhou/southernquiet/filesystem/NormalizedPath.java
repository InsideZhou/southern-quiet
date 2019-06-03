package me.insidezhou.southernquiet.filesystem;

import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;

import static me.insidezhou.southernquiet.filesystem.FileSystem.PATH_SEPARATOR_STRING;


/**
 * 规格化的路径。
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class NormalizedPath implements Serializable {
    private final static long serialVersionUID = -8700923055584230554L;

    public final static NormalizedPath root = new NormalizedPath("");

    public NormalizedPath(String path) {
        if (!StringUtils.hasText(path)) return;
        if ("".equalsIgnoreCase(path)) return;

        String p = path.replace("\\", PATH_SEPARATOR_STRING);

        String[] pathElements = Stream.of(p.split(PATH_SEPARATOR_STRING))
            .filter(item -> StringUtils.hasText(item))
            .toArray(String[]::new);

        if (0 == pathElements.length) return;
        if (1 == pathElements.length) {
            this.setName(pathElements[0]);
            return;
        }

        int lastIndex = pathElements.length - 1;
        this.setParentNames(Arrays.copyOf(pathElements, lastIndex));
        this.setName(pathElements[lastIndex]);
    }

    public NormalizedPath(String[] pathElements) {
        if (0 == pathElements.length) return;

        int lastIndex = pathElements.length - 1;
        this.setParentNames(Arrays.copyOf(pathElements, lastIndex));
        this.setName(pathElements[lastIndex]);
    }

    /**
     * 路径父级的名称集合。当前路径是根路径或一级路径时，为空集合。
     */
    private String[] parentNames = new String[0];
    /**
     * 路径名称。当前路径是根路径时，为空字符串。
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
                return StringUtils.isEmpty(name) ? "" : PATH_SEPARATOR_STRING;
            case 1:
                return PATH_SEPARATOR_STRING + parentNames[0];
            default:
                return PATH_SEPARATOR_STRING + String.join(PATH_SEPARATOR_STRING, parentNames);
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
        String parent = getParent();
        return parent.equals(PATH_SEPARATOR_STRING) ? PATH_SEPARATOR_STRING + name : parent + PATH_SEPARATOR_STRING + name;
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
