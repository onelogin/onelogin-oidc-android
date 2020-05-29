package com.onelogin.oidc.data.stores

/**
 * Implement this class in case that you want to override the behaviour of the storage in the library
 */
interface Store {
    /**
     * Takes an identifier and the data that will be stored
     *
     * @param key Identifier of the data that will be stored
     * @param data Data to be stored already serialized and encrypted
     * @return Whether or not the persistence was successful
     */
    fun persist(key: String, data: String): Boolean

    /**
     * Gets the information stored under the provided key, in case that the information doesn't exist
     * it should return null
     *
     * @param key Identifier of the data that we wan to fetch
     * @return String representing the data serialized and encrypted
     */
    fun fetch(key: String): String?

    /**
     * Removes the value of the specified key from the persisted storage
     *
     * @param key Identifier of the value that we want to remove from the storage
     */
    fun clear(key: String)
}
