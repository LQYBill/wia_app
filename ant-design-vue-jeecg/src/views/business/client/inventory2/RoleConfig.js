export default {
  admin: {
    columns: [
      {
        title: '#',
        key: 'rowIndex',
        width: 60,
        align: 'center',
        customRender: (t, r, index) => parseInt(index) + 1
      },
      {
        title: 'Client Code',
        align: 'center',
        dataIndex: 'clientId_dictText',
      },
      {
        title: 'Picture',
        align: 'center',
        dataIndex: 'imageSource',
        scopedSlots: {customRender: 'imgSlot'}
      },
      {
        title: 'ERP Code',
        align: 'center',
        dataIndex: 'erpCode',
      },
      {
        title: 'Name',
        align: 'center',
        dataIndex: 'productId_dictText',
      },
      {
        title: '库存数量',
        align: 'center',
        dataIndex: 'availableAmount',
      },
      {
        title: '在途数量',
        dataIndex: 'redQuantity',
        align: 'center',
        scopedSlots: {customRender: 'redAndGreenCell'}
      },
      {
        title: '平台单数量',
        align: 'center',
        dataIndex: 'platformOrderQuantity',
        customRender: (t, r, ibdex) => {
          if (t === null) return 0; else return t
        }
      }
    ],
    okText: "Proceed",
    okHandler: function (keys, records, comp) {
    },
  },
  client: {
    columns: [
      {
        title: '#',
        key: 'rowIndex',
        width: 60,
        align: 'center',
        customRender: (t, r, index) => parseInt(index) + 1
      },
      {
        title: 'Picture',
        align: 'center',
        dataIndex: 'imageSource',
        scopedSlots: {customRender: 'imgSlot'}
      },
      {
        title: 'ERP Code',
        align: 'center',
        dataIndex: 'erpCode',
      },
      {
        title: 'Name',
        align: 'center',
        dataIndex: 'productId_dictText',
      },
      {
        title: '库存数量',
        align: 'center',
        dataIndex: 'availableAmount',
      },
      {
        title: '在途数量',
        dataIndex: 'redQuantity',
        align: 'center',
        scopedSlots: {customRender: 'redAndGreenCell'}
      },
      {
        title: '平台单数量',
        align: 'center',
        dataIndex: 'platformOrderQuantity',
        customRender: (t, r, ibdex) => {
          if (t === null) return 0; else return t
        }
      }
    ],
    okText: "Proceed",
    okHandler: function (keys, records, comp) {
      if (keys.length < 1) {
        comp.$message.error("You should select at least 1 item to continue")
        return
      }
      let skusToBuy = records.map(r => (r['id']))
      console.log(skusToBuy)
      comp.$refs.popup.display(skusToBuy)
    },
  },
  'Client (for test)': {
    columns: [
      {
        title: '#',
        key: 'rowIndex',
        width: 60,
        align: 'center',
        customRender: (t, r, index) => parseInt(index) + 1
      },
      {
        title: 'Picture',
        align: 'center',
        dataIndex: 'imageSource',
        scopedSlots: {customRender: 'imgSlot'}
      },
      {
        title: 'ERP Code',
        align: 'center',
        dataIndex: 'erpCode',
      },
      {
        title: 'Name',
        align: 'center',
        dataIndex: 'productId_dictText',
      },
      {
        title: '库存数量',
        align: 'center',
        dataIndex: 'availableAmount',
      },
      {
        title: '在途数量',
        dataIndex: 'redQuantity',
        align: 'center',
        scopedSlots: {customRender: 'redAndGreenCell'}
      },
      {
        title: '平台单数量',
        align: 'center',
        dataIndex: 'platformOrderQuantity',
        customRender: (t, r, ibdex) => {
          if (t === null) return 0; else return t
        }
      }
    ],
    okText: "Proceed",
    okHandler: function (keys, records, comp) {
      if (keys.length < 1) {
        comp.$message.error("You should select at least 1 item to continue")
        return
      }
      let skusToBuy = records.map(r => (r['id']))
      console.log(skusToBuy)
      console.log(comp.$refs)
      comp.$refs["popup"].display(skusToBuy)
    },
    cancelText:"Cancel",
    cancelHandler(keys, records, comp){
      comp.clearSelected()
    }
  },
}