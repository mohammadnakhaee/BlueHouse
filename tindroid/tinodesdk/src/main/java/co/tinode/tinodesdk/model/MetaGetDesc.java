package co.tinode.tinodesdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

/**
 * Parameter of GetMeta.
 */
@JsonInclude(NON_DEFAULT)
public class MetaGetDesc implements Serializable {
    // ims = If modified since...
    public Date ims;

    public MetaGetDesc() {}

    @Override
    public String toString() {
        return "ims=" + ims;
    }
}