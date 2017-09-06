import React, { Component } from 'react'
import PropTypes from 'prop-types'
import { Form, FormGroup, ControlLabel, Button }
  from 'react-bootstrap'
import { Icon, TextInput, SelectableDropdown } from '../../components'
import Toggle from 'react-toggle'
/**
 * Reject Translations Administration panel
 */
export const MINOR = 'Minor'
export const MAJOR = 'Major'
export const CRITICAL = 'Critical'
const DO_NOT_RENDER = undefined

function priorityToTextState (priority) {
  switch (priority) {
    case CRITICAL:
      return 'text-danger'
    case MAJOR:
      return 'text-warning'
    case MINOR:
      return 'text-info'
  }
}

const priorityToDisplay =
  p => <span className={priorityToTextState(p)}>{p}</span>

class RejectionsForm extends Component {
  static propTypes = {
    entityId: PropTypes.number,
    priority: PropTypes.oneOf([
      MINOR,
      MAJOR,
      CRITICAL
    ]).isRequired,
    criteriaPlaceholder: PropTypes.string.isRequired,
    onSave: PropTypes.func.isRequired,
    onDelete: PropTypes.func,
    editable: PropTypes.bool,
    // if it's in add new item mode, we will allow user to update editable
    isNewItemMode: PropTypes.bool.isRequired,
    className: PropTypes.string,
    criterionId: PropTypes.string.isRequired
  }

  static defaultProps = {
    criterionId: 'review-criteria',
    isNewItemMode: false,
    onSave: () => {},
    onDelete: () => {}
  }

  constructor (props) {
    super(props)
    this.state = {
      description: '',
      isEditable: true,
      priority: this.props.priority
    }
  }

  onEditableChange = e => {
    const checked = e.target.checked
    this.setState(prevState => ({
      isEditable: checked
    }))
  }
  onTextChange = e => {
    const text = e.target.value
    this.setState(prevState => ({
      description: text
    }))
  }
  onPriorityChange = p => {
    this.setState(prevState => ({
      priority: p
    }))
  }
  onSave = () => {
    this.props.onSave(this.state)
  }
  onDelete = () => {
    this.props.onDelete(this.props.entityId)
  }

  render () {
    const {
      editable,
      className,
      isNewItemMode,
      criteriaPlaceholder,
      criterionId
    } = this.props
    const textState = priorityToTextState(this.state.priority)
    const title = <span className={textState}>{this.state.priority}</span>
    const priorityDisabled = !editable && !isNewItemMode
    const deleteBtn = isNewItemMode ? DO_NOT_RENDER
      : (
      <Button bsStyle='danger' className={className} onClick={this.onDelete}>
        <Icon name='trash' className='s0 editicon' />
      </Button>
      )
    const editableToggle = isNewItemMode ? (
      <FormGroup controlId='formInlineEditable'>
        <ControlLabel>Editable</ControlLabel><br />
        <Toggle icons={false} onChange={this.onEditableChange}
          checked={this.state.isEditable} />
      </FormGroup>
      )
      : DO_NOT_RENDER
    return (
      <Form className='rejections' inline>
        <FormGroup className='flex-grow1' controlId='formInlineCriteria'>
          <ControlLabel>Criteria</ControlLabel><br />
          <TextInput multiline editable={editable}
            type='text' numberOfLines={2} onChange={this.onTextChange}
            placeholder={criteriaPlaceholder} value={this.state.description} />
        </FormGroup>
        <FormGroup controlId='formInlinePriority'>
          <ControlLabel>Priority</ControlLabel><br />
          <SelectableDropdown
            id={criterionId + 'review-criteria-dropdown-basic'}
            onSelectDropdownItem={this.onPriorityChange}
            selectedValue={this.state.priority}
            title={title}
            valueToDisplay={priorityToDisplay}
            values={[MINOR, MAJOR, CRITICAL]}
            disabled={priorityDisabled}
          />
        </FormGroup>
        {editableToggle}
        <FormGroup controlId='formInlineButtonEdit'>
          <ControlLabel>&nbsp;</ControlLabel><br />
          <Button bsStyle='primary' className={className} onClick={this.onSave}
            disabled={!editable && !isNewItemMode}>
            <Icon name='edit' className='s0 editicon' />
          </Button>
          {deleteBtn}
        </FormGroup>
      </Form>
    )
  }
}

export default RejectionsForm
