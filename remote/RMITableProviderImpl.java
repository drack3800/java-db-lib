package ru.fizteh.fivt.students.dmitry_persiyanov.remote;

import ru.fizteh.fivt.storage.structured.TableProvider;

/**
 * Created by drack3800 on 20.12.2014.
 */
public class RMITableProviderImpl {
    private TableProvider tableProvider;

    RMITableProviderImpl(final TableProvider tableProvider) {
        this.tableProvider = tableProvider;
    }


}
