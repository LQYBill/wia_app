export default {
  /**
   * role code is the code in sys_role
   */
  roleCode1: {
    columns: [], // same as ant vue design / table / columns
    okText: "a string",
    /**
     * Event handler when click on OK button.
     * @param keys keys of row selected
     * @param records whole lines of row selected
     * @param comp the component instance (this)
     */
    okHandler: function (keys, records, comp) {
      /* ... */
    },
    cancelText: "a string",
    /**
     * Event handler when click on Cancel button.
     *
     * @param keys keys of row selected
     * @param records whole lines of row selected
     * @param comp the component instance (this)
     */
    cancelHandler(keys, records, comp) {
      /* .. */
    }
  },
  roleCode2: {
    columns: [],
    okText: "a string",
    okHandler: function (keys, records, comp) {
      /* ... */
    },
    cancelText: "a string",
    cancelHandler(keys, records, comp) {
      /* .. */
    }
  },
}